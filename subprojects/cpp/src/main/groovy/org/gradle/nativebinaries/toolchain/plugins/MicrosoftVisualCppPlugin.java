package org.gradle.nativebinaries.toolchain.plugins;

import org.gradle.api.Incubating;
import org.gradle.api.NamedDomainObjectFactory;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.internal.file.FileResolver;
import org.gradle.internal.os.OperatingSystem;
import org.gradle.internal.reflect.Instantiator;
import org.gradle.internal.service.ServiceRegistry;
import org.gradle.model.Mutate;
import org.gradle.model.RuleSource;
import org.gradle.nativebinaries.plugins.NativeComponentModelPlugin;
import org.gradle.nativebinaries.toolchain.VisualCpp;
import org.gradle.nativebinaries.toolchain.internal.ToolChainRegistryInternal;
import org.gradle.nativebinaries.toolchain.internal.msvcpp.VisualCppToolChain;
import org.gradle.nativebinaries.toolchain.internal.msvcpp.VisualStudioLocator;
import org.gradle.nativebinaries.toolchain.internal.msvcpp.WindowsSdkLocator;
import org.gradle.process.internal.ExecActionFactory;

/**
 * A {@link Plugin} which makes the Microsoft Visual C++ compiler available to compile C/C++ code.
 */
@Incubating
public class MicrosoftVisualCppPlugin implements Plugin<Project> {

    public void apply(Project project) {
        project.getPlugins().apply(NativeComponentModelPlugin.class);
    }

    @RuleSource
    public static class Rules {
        @Mutate
        public static void addGccToolChain(ToolChainRegistryInternal toolChainRegistry, ServiceRegistry serviceRegistry) {
            final FileResolver fileResolver = serviceRegistry.get(FileResolver.class);
            final ExecActionFactory execActionFactory = serviceRegistry.get(ExecActionFactory.class);
            final Instantiator instantiator = serviceRegistry.get(Instantiator.class);
            final OperatingSystem operatingSystem = serviceRegistry.get(OperatingSystem.class);
            final VisualStudioLocator visualStudioLocator = serviceRegistry.get(VisualStudioLocator.class);
            final WindowsSdkLocator windowsSdkLocator = serviceRegistry.get(WindowsSdkLocator.class);

            toolChainRegistry.registerFactory(VisualCpp.class, new NamedDomainObjectFactory<VisualCpp>() {
                public VisualCpp create(String name) {
                    return instantiator.newInstance(VisualCppToolChain.class, name, operatingSystem, fileResolver, execActionFactory, visualStudioLocator, windowsSdkLocator, instantiator);
                }
            });
            toolChainRegistry.registerDefaultToolChain(VisualCppToolChain.DEFAULT_NAME, VisualCpp.class);
        }

    }
}
