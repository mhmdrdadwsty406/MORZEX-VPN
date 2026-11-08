const { withAppBuildGradle } = require("@expo/config-plugins");

module.exports = function withMorzexDesugaring(config) {
  return withAppBuildGradle(config, (config) => {
    let s = config.modResults.contents;

    if (!s.includes("coreLibraryDesugaringEnabled true")) {
      s = s.replace(
        "android {",
        "android {\n    compileOptions {\n        sourceCompatibility JavaVersion.VERSION_17\n        targetCompatibility JavaVersion.VERSION_17\n        coreLibraryDesugaringEnabled true\n    }"
      );
    }

    if (!s.includes("desugar_jdk_libs")) {
      s = s.replace(
        "dependencies {",
        "dependencies {\n    coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:2.1.5'"
      );
    }

    config.modResults.contents = s;
    return config;
  });
};
