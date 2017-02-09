/**
 * Copyright 2011-2015 GatlingCorp (http://gatling.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jenkinsci.plugins.valgrind.steps;

import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.valgrind.ValgrindPublisher;
import org.jenkinsci.plugins.valgrind.config.ValgrindPublisherConfig;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.inject.Inject;

public class ValgrindPublisherStepExecution extends AbstractSynchronousNonBlockingStepExecution<Void> {
    
 
    @StepContextParameter
    private transient TaskListener listener;

    @StepContextParameter
    private transient Run build;

    @StepContextParameter
    private transient Launcher launcher;
    @StepContextParameter
    private transient FilePath ws;

    @Inject
    private ValgrindPublisherStep step;

    

    @Override
    protected Void run() throws Exception {
        System.out.println("Running Valgrind publisher step");

        ValgrindPublisherConfig valgrindPublisherConfig = step.getValgrindPublisherConfig();
	ValgrindPublisher publisher = new ValgrindPublisher(valgrindPublisherConfig.getPattern(),
						valgrindPublisherConfig.getFailThresholdInvalidReadWrite(),
						valgrindPublisherConfig.getFailThresholdDefinitelyLost(),
						valgrindPublisherConfig.getFailThresholdTotal(),
						valgrindPublisherConfig.getUnstableThresholdInvalidReadWrite(),
						valgrindPublisherConfig.getUnstableThresholdDefinitelyLost(),
						valgrindPublisherConfig.getUnstableThresholdTotal(),
						valgrindPublisherConfig.isPublishResultsForAbortedBuilds(),
						valgrindPublisherConfig.isPublishResultsForFailedBuilds(),
						valgrindPublisherConfig.isFailBuildOnMissingReports(),
						valgrindPublisherConfig.isFailBuildOnInvalidReports()
					);
        publisher.perform(build, ws, launcher, listener);
        return null;
    }
}
