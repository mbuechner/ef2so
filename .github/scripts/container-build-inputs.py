import hashlib
import json
import os
from pathlib import Path
import re
import subprocess


BUILD_INPUTS_LABEL = "de.ddb.labs.ef2so.build-inputs"
DIGEST = re.compile(r"sha256:[0-9a-f]{64}")
IMAGE_NAME = re.compile(r"[a-z0-9][a-z0-9./:_-]*")
IMAGE_TAG = re.compile(r"[a-zA-Z0-9_][a-zA-Z0-9_.-]{0,127}")
FROM = re.compile(
    r"^\s*FROM\s+(?:--platform=(\S+)\s+)?(\S+)(?:\s+AS\s+([a-z0-9][a-z0-9_.-]*))?\s*$",
    re.IGNORECASE,
)


def inspect(image, *options, allow_missing=False):
    result = subprocess.run(
        ["docker", "buildx", "imagetools", "inspect", *options, image],
        capture_output=True,
        timeout=90,
        check=False,
    )
    if result.returncode:
        error = result.stderr.decode(errors="replace").strip()
        if allow_missing and (
            f"{image.lower()}: not found" in error.lower() or "manifest unknown" in error.lower()
        ):
            return None
        raise SystemExit(error or f"Could not inspect {image}.")
    return result.stdout


def base_images(dockerfile, platform):
    images = []
    stages = {}
    runtime = None
    for line in dockerfile.read_text(encoding="utf-8-sig").splitlines():
        if not re.match(r"^\s*FROM\b", line, re.IGNORECASE):
            continue
        match = FROM.fullmatch(line)
        if not match:
            raise SystemExit(f"Unsupported Dockerfile instruction: {line}")
        from_platform, image, stage = match.groups()
        if from_platform not in (None, platform, "$TARGETPLATFORM", "${TARGETPLATFORM}"):
            raise SystemExit(f"Unsupported platform in Dockerfile instruction: {line}")

        if image.lower() == "scratch":
            runtime = None
        elif image.lower() in stages:
            runtime = stages[image.lower()]
        else:
            reference, separator, pinned = image.partition("@")
            name, colon, tag = reference.rpartition(":")
            has_tag = bool(colon and IMAGE_NAME.fullmatch(name) and IMAGE_TAG.fullmatch(tag))
            has_digest = bool(separator and IMAGE_NAME.fullmatch(reference) and DIGEST.fullmatch(pinned))
            if (separator and not has_digest) or not (has_tag or has_digest):
                raise SystemExit(f"Base images must use literal tags or digests: {image}.")
            images.append(image)
            runtime = image
        if stage:
            stages[stage.lower()] = runtime

    if not images:
        raise SystemExit(f"No external base images found in {dockerfile}.")
    return list(dict.fromkeys(images)), runtime


def sha256_manifest(manifest):
    return "sha256:" + hashlib.sha256(manifest).hexdigest()


def pinned_reference(image, digest):
    return f"{image.split('@', 1)[0]}@{digest}"


def matches_platform(config, target):
    os_name, architecture, *variant = target
    return (
        config.get("os") == os_name
        and config.get("architecture") == architecture
        and (not variant or config.get("variant") == variant[0])
    )


def base_digest(image, target):
    raw = inspect(image, "--raw")
    manifest = json.loads(raw)
    if "manifests" not in manifest:
        digest = sha256_manifest(raw)
        config = json.loads(inspect(pinned_reference(image, digest), "--format", "{{json .Image}}"))
        if config and matches_platform(config, target):
            return digest
    else:
        matches = {
            entry["digest"]
            for entry in manifest["manifests"]
            if matches_platform(entry.get("platform") or {}, target)
            and DIGEST.fullmatch(entry.get("digest", ""))
        }
        if len(matches) == 1:
            return matches.pop()
        if matches:
            raise SystemExit(f"Multiple manifests for {'/'.join(target)} in {image}.")
    raise SystemExit(f"No image manifest for {'/'.join(target)} in {image}.")


def reusable_image(image, fingerprint, target):
    raw = inspect(image, "--raw", allow_missing=True)
    if raw is None:
        return None

    # Older multi-platform tags are rebuilt so we never promote unscanned platforms.
    entries = json.loads(raw).get("manifests")
    if entries is not None and (
        len(entries) != 1 or not matches_platform(entries[0].get("platform") or {}, target)
    ):
        return None
    # Read the image config from the same immutable digest that will be scanned.
    digest = sha256_manifest(raw)
    config = json.loads(inspect(pinned_reference(image, digest), "--format", "{{json .Image}}"))
    if config and matches_platform(config, target):
        labels = (config.get("config") or {}).get("Labels") or {}
        if labels.get(BUILD_INPUTS_LABEL) == fingerprint:
            return digest
    return None


def main():
    platform = os.environ.get("TARGET_PLATFORM", "linux/amd64")
    target = platform.split("/")
    if not re.fullmatch(r"[a-z0-9]+/[a-z0-9]+(?:/[a-z0-9]+)?", platform):
        raise SystemExit(f"Invalid target platform: {platform}.")
    names, runtime = base_images(Path("release-source/Dockerfile"), platform)
    bases = {name: base_digest(name, target) for name in names}
    inputs = json.dumps([os.environ["RELEASE_REVISION"], platform, list(bases.items())]).encode()
    fingerprint = hashlib.sha256(inputs).hexdigest()
    release_image = f"{os.environ['IMAGE']}:{os.environ['RELEASE_TAG']}"
    reusable = reusable_image(release_image, fingerprint, target)

    with Path(os.environ["GITHUB_OUTPUT"]).open("a", encoding="utf-8") as output:
        if reusable:
            print(f"reusable_digest={reusable}", file=output)
            print(f"Reusing {release_image}@{reusable} with unchanged build inputs.")
            return

        if runtime:
            print(f"base_name={runtime}", file=output)
            print(f"base_digest={bases[runtime]}", file=output)
        print(f"fingerprint={fingerprint}", file=output)
        print("contexts<<BASE_IMAGES", file=output)
        for name, digest in bases.items():
            if "@" not in name:
                print(f"{name}=docker-image://{pinned_reference(name, digest)}", file=output)
        print("BASE_IMAGES", file=output)


if __name__ == "__main__":
    main()
