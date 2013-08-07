package org.jenkinsci.plugins.valgrind.util;

import java.io.IOException;

import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.model.Actionable;
import hudson.model.HealthReportingAction;
import hudson.model.Result;

import org.kohsuke.stapler.StaplerProxy;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;


public abstract class AbstractValgrindBuildAction extends Actionable implements Action, HealthReportingAction, StaplerProxy
{
    protected AbstractBuild<?, ?> owner;

    protected AbstractValgrindBuildAction(AbstractBuild<?, ?> owner) {
        this.owner = owner;
    }

    @SuppressWarnings("unchecked")
	public <T extends AbstractValgrindBuildAction> T getPreviousResult() {
        AbstractBuild<?, ?> b = owner;
        while (true) {
            b = b.getPreviousBuild();
            if (b == null)
                return null;
            if (b.getResult() == Result.FAILURE)
                continue;
            AbstractValgrindBuildAction r = b.getAction(this.getClass());
            if (r != null)
                return (T) r;
        }
    }

    public AbstractBuild<?, ?> getOwner() {
        return owner;
    }

    /**
     * @param request
     * @param response
     * @throws IOException
     * @throws InterruptedException
     */
    public abstract void doGraph(StaplerRequest req, StaplerResponse rsp) throws IOException, InterruptedException;

}
