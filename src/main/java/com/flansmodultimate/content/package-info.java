/**
 * Content-pack discovery, type loading, asset generation and resource-pack integration.
 * The supported addon-facing contracts remain in {@code com.flansmodultimate.api}.
 * Packaged mods continue to register through {@link com.flansmodultimate.PackagedContentPackApi}.
 * Asset generators receive their inputs explicitly and do not own registry or loading state.
 */
package com.flansmodultimate.content;
