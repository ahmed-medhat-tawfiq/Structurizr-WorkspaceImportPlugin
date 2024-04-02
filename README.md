# Structurizr-WorkspaceImportPlugin

Structurizr Plugin to import multiple workspaces in your workspace.

## Usage

Download and Copy the JAR file from the latest release https://github.com/ahmed-medhat-tawfiq/Structurizr-WorkspaceImportPlugin/releases to your plugins folder https://docs.structurizr.com/dsl/plugins

Use it by CLI https://docs.structurizr.com/cli

Then in your parent workspace, you can import multiple workspaces like this:

```dsl
    !plugin WorkspaceImportPlugin {
        paths "./"
        include "softwareSystem.* , container.* , component.*"
        ccprefix true
    }
```

- **paths (Required):** Array of paths to search for workspaces
  - i.e. "./" will search in the current directory
  - NOTE: The plugin will search recursively in the given paths and subdirectories for each path.
- **include (Optional):** Regex to include workspaces. 
  - Default is "*" (all). 
  - i.e. container.* will include all containers in included workspaces
  - i.e container.someContainer will include only container.someContainer
- **ccprefix (Optional):** Boolean to include the workspace name as a prefix in the imported components, containers, and software systems.
  - Default is false
  - i.e. If true, and the workspace name is "workspace1", the component "component1" will be imported as "[workspace1] component1"
