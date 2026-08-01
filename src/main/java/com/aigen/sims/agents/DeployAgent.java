package com.aigen.sims.agents;

/**
 * DeployAgent — plans and executes deployment after a collective APPROVE.
 */
public class DeployAgent extends SLMAgent {
    public DeployAgent(String modelName) { super(modelName); }

    /** Produce a deploy plan (steps) for approved work. */
    public String planDeploy(String workDescription) throws Exception {
        String prompt = "You are DeployAgent. Create a numbered deployment plan for the approved work.\n"
                + "Include: build, test, backup/rollback, deploy, verify.\n"
                + "Work: " + workDescription;
        return generate(prompt, 300);
    }

    /** Gate check: is it safe to deploy? Returns JSON verdict. */
    public String gateCheck(String plan, String status) throws Exception {
        String prompt = "You are DeployAgent gate. Is it safe to deploy given plan and status?\n"
                + "Answer STRICTLY as JSON: {\"safe\": true/false, \"reason\": \"...\"}\n"
                + "Plan: " + plan + "\nStatus: " + status;
        return generate(prompt, 150);
    }
}
