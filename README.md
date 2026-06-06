# PaperWinter Template Plugin

This is a template for creating PaperMC plugins using the [Winter Framework](https://github.com/thewinterframework/core).

## Structure

- **Main Class**: `me.mapacheee.template.TemplatePlugin`
- **Services**: `me.mapacheee.template.service` (Business logic, config access)
- **Commands**: `me.mapacheee.template.command` (Incendo Cloud commands)
- **Listeners**: `me.mapacheee.template.listener` (Bukkit listeners)

## Getting Started

1.  **Update Dependencies**: Check `build.gradle.kts` and update plugin name.
2.  **Rename Package**: Refactor the `me.mapacheee.template` package to your desired package name.
3.  **Update plugin.yml**: Edit `src/main/resources/paper-plugin.yml` with your plugin name and description.

## Building

Run `./gradlew clean shadowJar` to build the plugin.
