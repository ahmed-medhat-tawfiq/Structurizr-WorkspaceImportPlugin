import com.structurizr.Workspace;
import com.structurizr.dsl.StructurizrDslParser;
import com.structurizr.dsl.StructurizrDslParserException;
import com.structurizr.dsl.StructurizrDslPlugin;
import com.structurizr.dsl.StructurizrDslPluginContext;
import com.structurizr.model.*;
import com.structurizr.view.SystemLandscapeView;
import com.structurizr.view.ViewSet;

import java.io.File;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * This example plugin loads a given workspace and clones top-level relationships into this workspace.
 *
 * Possible enhancements to this example include:
 *  - Include relationship technologies.
 *  - Recursively load all .dsl files in a given directory.
 */
public class WorkspaceImportPlugin implements StructurizrDslPlugin {

    private static final String PATHS = "paths";
    private static final String INCLUDE = "include";
    private static final String CCPREFIX = "ccprefix";

    @Override
    public void run(StructurizrDslPluginContext context) {
        String includes = context.getParameter(INCLUDE);
        String ccprefix = context.getParameter(CCPREFIX);
        Boolean useCCPrefix = ccprefix != null ? Boolean.parseBoolean(ccprefix) : false;
        String[] includeList = new String[0];

        if (includes != null) {
            includeList = includes != null ? Arrays.stream(includes.split(",")).map(String::trim).toArray(String[]::new) : new String[1];
        }
        else {
            includeList = new String[1];
            includeList[0] = "*";
        }

        System.out.println("Importing workspaces," + " From paths: " + context.getParameter(PATHS) + ", Including: " + Arrays.toString(includeList));
        System.out.println("Using Workspace Name Prefix for Containers and Components Names Enabled= " + useCCPrefix);

        Workspace[] workspaces = loadDirWorkspaces(context);

        if (workspaces.length == 0) {
            System.out.println("No workspaces found.");
            return;
        }

        for (Workspace workspace : workspaces) {
            System.out.println("Importing workspace: " + workspace.getName());

            Set<Relationship> includedRelationships = workspace.getModel().getRelationships();
            Set<SoftwareSystem> includedSystems = workspace.getModel().getSoftwareSystems();
            Model model = context.getWorkspace().getModel();
    
           for (SoftwareSystem system : includedSystems) {
                cloneElementIfItDoesNotExist(workspace, system, model, includeList, useCCPrefix);
            }
            
            for (Relationship relationship : includedRelationships) {
                cloneRelationshipIfItDoesNotExist(workspace, relationship, model, useCCPrefix);
            }
        }
    }


    private Workspace[] loadDirWorkspaces(StructurizrDslPluginContext context) {
        File directory = context.getDslFile().getParentFile();
        String paths = context.getParameter(PATHS);
        Set<Workspace> workspaces = new HashSet<>();
        String[] pathsArray = paths != null ? paths.split("[,\\s]+") : new String[0];

        for (String path : pathsArray) {
            if (path != null) {
                File file = new File(directory, path);
                if (file.exists()) {
                    if (file.isDirectory()) {
                        File[] files = directory.listFiles((dir, name) -> name.endsWith(".dsl"));
                
                        for (int i = 0; i < files.length; i++) {
                            Workspace loadedWorkspace = getFileWorkspace(context, files[i]);
    
                            if (loadedWorkspace != null) {
                                workspaces.add(loadedWorkspace);
                            }
                        }
                    } else {
                        Workspace loadedWorkspace = getFileWorkspace(context, file);
    
                        if (loadedWorkspace != null) {
                            workspaces.add(loadedWorkspace);
                        }
                    }
                }
            }
        }

        return workspaces.toArray(new Workspace[0]);
    }

    private Workspace getFileWorkspace(StructurizrDslPluginContext context, File file) {
        try {
            // don't load the current workspace file
            if (file.getName().equals(context.getDslFile().getName())) {
                return null;
            }

            System.out.println("Loading workspace from file: " + file.getName());
            StructurizrDslParser dslParser = new StructurizrDslParser();

            dslParser.parse(file);
            Workspace workspace = dslParser.getWorkspace();

            return workspace;
        } catch (StructurizrDslParserException e) {
            System.out.println(e);
            return null;
        }
    }

    private void cloneRelationshipIfItDoesNotExist(Workspace workspace, Relationship relationship, Model model, Boolean useCCPrefix) {
        Relationship clonedRelationship = null;
        StaticStructureElement source = null;
        StaticStructureElement destination = null;
        String prefix = workspace.getName() == null || !useCCPrefix ? "" : "["+workspace.getName()+"] ";


        if (source == null) source = model.getSoftwareSystemWithName(relationship.getSource().getName());
        if (destination == null) destination = model.getSoftwareSystemWithName(relationship.getDestination().getName());

        if (source == null) source = model.getPersonWithName(relationship.getSource().getName());
        if (destination == null) destination = model.getPersonWithName(relationship.getDestination().getName());

        for (SoftwareSystem softwareSystem : model.getSoftwareSystems()) {
            String systemName = softwareSystem.getName();

            if (source == null) source = softwareSystem.getContainerWithName(prefix+relationship.getSource().getName());
            if (destination == null) destination = softwareSystem.getContainerWithName(prefix+relationship.getDestination().getName());

            for (Container container : softwareSystem.getContainers()) {
                if (source == null) source = container.getComponentWithName(prefix+relationship.getSource().getName());
                if (destination == null) destination = container.getComponentWithName(prefix+relationship.getDestination().getName());
            }
        }
        
        if (source != null && destination != null && !source.hasEfferentRelationshipWith(destination)) {
            System.out.println("Cloning relationship: " + source.getName() + " -> " + destination.getName());
            if (destination instanceof SoftwareSystem) {
                clonedRelationship = source.uses((SoftwareSystem) destination, relationship.getDescription());
            } else if (destination instanceof Person) {
                clonedRelationship = source.delivers((Person) destination, relationship.getDescription());
            } else if (destination instanceof Container) {
                clonedRelationship = source.uses((Container) destination, relationship.getDescription());
            } else if (destination instanceof Component) {
                clonedRelationship = source.uses((Component) destination, relationship.getDescription());
            }
        }

        if (clonedRelationship != null) {
            clonedRelationship.addTags(relationship.getTags());
        }
    }

    private void cloneElementIfItDoesNotExist(Workspace workspace, SoftwareSystem system, Model model, String[] includeList, Boolean useCCPrefix) {
        String[] includedSystems = filterElements(includeList, "softwareSystem");
        String[] includedContainers = filterElements(includeList, "container");
        String[] includedComponents = filterElements(includeList, "component");
        String prefix = workspace.getName() == null || !useCCPrefix ? "" : "["+workspace.getName()+"] ";

        if (includeList.length != 0) {
            if (!Arrays.asList(includedSystems).contains("*") && !Arrays.asList(includedSystems).contains(system.getName())) {
                // includedSystems to string coma separated
                System.out.println("Skipping system: " + system.getName() + ", because it is not included in the list "+ Arrays.toString(includedSystems));
                return;
            }
        }
        

        System.out.println("Cloning system: " + system.getName());

        SoftwareSystem existingSystem = model.getSoftwareSystemWithName(system.getName());
        if (existingSystem == null) {
            model.addSoftwareSystem(system.getName(), system.getDescription());
        }
        else {
            if (system.getDescription() != null) existingSystem.setDescription(system.getDescription());
            if (system.getTags().length() != 0) existingSystem.addTags(system.getTags());
        }

        SoftwareSystem targetSystem = model.getSoftwareSystemWithName(system.getName());

        system.getContainers().forEach(container -> {
            if (includeList.length != 0) {
                if (!Arrays.asList(includedContainers).contains("*") && !Arrays.asList(includedContainers).contains(container.getName())) {
                    System.out.println("Skipping container: " + container.getName() + ", because it is not included in the list "+ Arrays.toString(includedContainers));
                    return;
                }
            }

            System.out.println("Cloning container: " + prefix+container.getName());

            Container existingContainer = targetSystem.getContainerWithName(container.getName());

            if (existingContainer == null) {
                targetSystem.addContainer(prefix+container.getName(), container.getDescription(), container.getTechnology());
            }
            else {
                if (container.getDescription() != null) existingContainer.setDescription(container.getDescription());
                if (container.getTechnology() != null) existingContainer.setTechnology(container.getTechnology());
                if (container.getTags().length() != 0) existingContainer.addTags(container.getTags());
            }

            Container targetContainer = targetSystem.getContainerWithName(container.getName());

            container.getComponents().forEach(component -> {
                if (includeList.length != 0) {
                    if (!Arrays.asList(includedComponents).contains("*") && !Arrays.asList(includedComponents).contains(component.getName())) {
                        System.out.println("Skipping component: " + component.getName() + ", because it is not included in the list "+ Arrays.toString(includedComponents));
                        return;
                    }
                }

                System.out.println("Cloning component: " + prefix+component.getName());

                Component existingComponent = targetContainer.getComponentWithName(component.getName());

                if (existingComponent == null) {
                    targetContainer.addComponent(prefix+component.getName(), component.getDescription(), component.getTechnology());
                }
                else {
                    if (component.getDescription() != null) existingComponent.setDescription(component.getDescription());
                    if (component.getTechnology() != null) existingComponent.setTechnology(component.getTechnology());
                    if (component.getTags().length() != 0) existingComponent.addTags(component.getTags());
                }
            });
        });
    }


    private String[] filterElements(String[] includeList, String prefix) {
        if (includeList == null) return new String[0];
        Set<String> filtered = new HashSet<>();
        for (String include : includeList) {
            if (include.equals("*") || include.equals(prefix + ".*") || include.equals(prefix) || include.equals(prefix + ".[*]")) {
                filtered.add("*");
            }
            else if (include.startsWith(prefix + ".[") && include.endsWith("]")) {
                // remove prefix and brackets softwareSystem.[Order Service] -> [Order Service]
                filtered.add(include.substring(prefix.length() + 2, include.length() - 1));
            }
        }

        return filtered.toArray(new String[0]);
    } 
}
