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

import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import java.io.Serializable;


import org.jenkinsci.plugins.valgrind.config.ValgrindPublisherConfig;

/**
 * Publisher for valgrind reports.
 */
public class ValgrindPublisherStep extends AbstractStepImpl implements Serializable{

    

    private ValgrindPublisherConfig valgrindPublisherConfig;
    
    @DataBoundConstructor
    public ValgrindPublisherStep(String pattern,
    				String failThresholdInvalidReadWrite, 
    				String failThresholdDefinitelyLost, 
    				String failThresholdTotal,
   				String unstableThresholdInvalidReadWrite, 
    				String unstableThresholdDefinitelyLost, 
    				String unstableThresholdTotal,
   				boolean publishResultsForAbortedBuilds,
    				boolean publishResultsForFailedBuilds,
    				boolean failBuildOnMissingReports,
    				boolean failBuildOnInvalidReports)
	{
		valgrindPublisherConfig = new ValgrindPublisherConfig(
				pattern, 
				failThresholdInvalidReadWrite, 
				failThresholdDefinitelyLost, 
				failThresholdTotal,
				unstableThresholdInvalidReadWrite, 
				unstableThresholdDefinitelyLost, 
				unstableThresholdTotal,
				publishResultsForAbortedBuilds,
				publishResultsForFailedBuilds,
				failBuildOnMissingReports,
				failBuildOnInvalidReports);		
	}	

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {
        public DescriptorImpl()
	{
	    super(ValgrindPublisherStepExecution.class);
	}

        @Override
        public String getFunctionName() {
            return "publishValgrind";
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return "Publish valgrind reports";
        }
    }
    public ValgrindPublisherConfig getValgrindPublisherConfig()
    {
        return valgrindPublisherConfig;
    }

    public void setValgrindPublisherConfig(ValgrindPublisherConfig valgrindPublisherConfig)
    {
        this.valgrindPublisherConfig = valgrindPublisherConfig;
    }    

    public String getPattern()
    {
        return valgrindPublisherConfig.getPattern();
    }

    public String getFailThresholdInvalidReadWrite()
    {
        return valgrindPublisherConfig.getFailThresholdInvalidReadWrite();
    }

    public String getFailThresholdDefinitelyLost()
    {
        return valgrindPublisherConfig.getFailThresholdDefinitelyLost();
    }

    public String getFailThresholdTotal()
    {
        return valgrindPublisherConfig.getFailThresholdTotal();
    }

    public String getUnstableThresholdInvalidReadWrite()
    {
        return valgrindPublisherConfig.getUnstableThresholdInvalidReadWrite();
    }

    public String getUnstableThresholdDefinitelyLost()
    {
        return valgrindPublisherConfig.getUnstableThresholdDefinitelyLost();
    }

    public String getUnstableThresholdTotal()
    {
        return valgrindPublisherConfig.getUnstableThresholdTotal();
    }

    public boolean isPublishResultsForAbortedBuilds()
    {
        return valgrindPublisherConfig.isPublishResultsForAbortedBuilds();
    }

    public boolean isPublishResultsForFailedBuilds()
    {
        return valgrindPublisherConfig.isPublishResultsForFailedBuilds();
    }
    
    public boolean isFailBuildOnMissingReports()
    {
        return valgrindPublisherConfig.isFailBuildOnMissingReports();
    }
    
    public boolean isFailBuildOnInvalidReports()
    {
        return valgrindPublisherConfig.isFailBuildOnInvalidReports();
    }
}
