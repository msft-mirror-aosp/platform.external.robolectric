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
  -x '*.sh' \
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
> /tmp/robo/patch

echo applying patch
patch -p1 < /tmp/robo/patch

echo patch applied
