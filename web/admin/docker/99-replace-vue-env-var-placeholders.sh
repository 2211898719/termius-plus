#!/bin/sh

ROOT_DIR=/app

for file in $ROOT_DIR/assets/*.js* $ROOT_DIR/assets/*.css $ROOT_DIR/index.html;
do
  sed -i 's|PLACEHOLDER_VITE_ASSETS_DOMAIN|'${VITE_ASSETS_DOMAIN}'|g' $file
done
