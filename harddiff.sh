#!/bin/bash
set -x
val=$(repo sync . | echo $?)
if  [ $val != 0 ]; then
  echo could not repo sync local directory, bailing.
fi

rm -rf /tmp/robo
mkdir -p /tmp/robo || true
pushd /tmp/robo
git clone -b google --single-branch https://github.com/robolectric/robolectric.git
#git checkout google
popd

echo building patch
# waiting on clean up the ttf/icu data -- hence nativeruntime resources are ignored.
diff -Naur . /tmp/robo/robolectric \
  -x '*.bp' \
  -x '*.md' \
  -x 'harddiff.sh' \
  -x 'METADATA' \
  -x 'MODULE_LICENSE_MIT' \
  -x 'NOTICE' \
  -x 'OWNERS' \
  -x 'soong*' \
  -x '.git*' \
  -x 'buildSrc' \
  -x 'gradle' \
  -x '*.gradle' \
  -x 'sdks.txt' \
  -x '*.ttf' \
  -x '*.utf' \
  -x '*.otf' \
  -x '*.ttc' \
  -x 'fonts.xml' \
  -x '*.dat' \
  -x 'lint-baseline.xml' \
  -x 'robo-manifest.xml' \
  -x 'FontAndroidManifest.xml' \
  -x 'config.xml' \
  -x 'public.xml' \
  -x 'AndroidManifest.xml' \
> /tmp/robo/patch

echo applying patch
patch -p1 < /tmp/robo/patch

echo patch applied

# How to auto revert an existing CL from upstream on top of the diff
# One per CL please.

# echo reverting upstream CL due to ...
# git diff 162eaf30e754fdc3322b2c6b0df6576b4555e650 162eaf30e754fdc3322b2c6b0df6576b4555e650^ | patch -p1
