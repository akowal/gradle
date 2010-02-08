/*
 * Copyright 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.external.junit;

import org.apache.commons.lang.StringUtils;
import org.gradle.api.Action;
import org.gradle.api.internal.tasks.testing.junit.AntJUnitTestClassProcessor;
import org.gradle.api.tasks.testing.AbstractTestTask;
import org.gradle.api.tasks.testing.JunitForkOptions;
import org.gradle.api.internal.tasks.testing.junit.AntJUnitReport;
import org.gradle.api.tasks.testing.junit.JUnitOptions;
import org.gradle.api.tasks.util.JavaForkOptions;
import org.gradle.api.testing.TestClassProcessor;
import org.gradle.api.testing.TestClassProcessorFactory;
import org.gradle.api.testing.fabric.AbstractTestFrameworkInstance;
import org.gradle.process.WorkerProcessBuilder;

import java.io.File;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * @author Tom Eyckmans
 */
public class JUnitTestFrameworkInstance extends AbstractTestFrameworkInstance {
    private AntJUnitReport antJUnitReport;
    private JUnitOptions options;
    private JUnitDetector detector;

    protected JUnitTestFrameworkInstance(AbstractTestTask testTask, JUnitTestFramework testFramework) {
        super(testTask, testFramework);
    }

    public void initialize() {
        antJUnitReport = new AntJUnitReport();
        options = new JUnitOptions((JUnitTestFramework) testFramework);

        final JunitForkOptions forkOptions = options.getForkOptions();

        forkOptions.setDir(testTask.getProject().getProjectDir());

        detector = new JUnitDetector(testTask.getTestClassesDir(), testTask.getClasspath());
    }

    public TestClassProcessorFactory getProcessorFactory() {
        final File testResultsDir = testTask.getTestResultsDir();
        return new TestClassProcessorFactoryImpl(testResultsDir);
    }

    public Action<WorkerProcessBuilder> getWorkerConfigurationAction() {
        return new Action<WorkerProcessBuilder>() {
            public void execute(WorkerProcessBuilder workerProcessBuilder) {
                workerProcessBuilder.sharedPackages("junit.framework");
                workerProcessBuilder.sharedPackages("org.junit");
            }
        };
    }

    public void report() {
        if (!testTask.isTestReport()) {
            return;
        }
        antJUnitReport.execute(testTask.getTestResultsDir(), testTask.getTestReportDir(),
                testTask.getProject().getAnt());
    }

    public JUnitOptions getOptions() {
        return options;
    }

    void setOptions(JUnitOptions options) {
        this.options = options;
    }

    AntJUnitReport getAntJUnitReport() {
        return antJUnitReport;
    }

    void setAntJUnitReport(AntJUnitReport antJUnitReport) {
        this.antJUnitReport = antJUnitReport;
    }

    public JUnitDetector getDetector() {
        return detector;
    }

    public void applyForkArguments(JavaForkOptions javaForkOptions) {
        final JunitForkOptions forkOptions = options.getForkOptions();

        if (StringUtils.isNotEmpty(forkOptions.getJvm())) {
            javaForkOptions.executable(forkOptions.getJvm());
        }

        if (forkOptions.getDir() != null) {
            javaForkOptions.workingDir(forkOptions.getDir());
        }

        if (StringUtils.isNotEmpty(forkOptions.getMaxMemory())) {
            javaForkOptions.setMaxHeapSize(forkOptions.getMaxMemory());
        }

        final List<String> jvmArgs = forkOptions.getJvmArgs();
        if (jvmArgs != null && !jvmArgs.isEmpty()) {
            javaForkOptions.jvmArgs(jvmArgs);
        }

        if (forkOptions.isNewEnvironment()) {
            final Map<String, String> environment = forkOptions.getEnvironment();
            javaForkOptions.setEnvironment(environment);
        }

        // TODO clone
        // TODO bootstrapClasspath - not sure which bootstrap classpath option to use:
        // TODO one of: -Xbootclasspath or -Xbootclasspath/a or -Xbootclasspath/p
        // TODO -Xbootclasspath/a seems the correct one - to discuss or improve and make it
        // TODO possible to specify which one to use. -> will break ant task compatibility in options.
    }

    private static class TestClassProcessorFactoryImpl implements TestClassProcessorFactory, Serializable {
        private final File testResultsDir;

        public TestClassProcessorFactoryImpl(File testResultsDir) {
            this.testResultsDir = testResultsDir;
        }

        public TestClassProcessor create() {
            return new AntJUnitTestClassProcessor(testResultsDir);
        }
    }
}
