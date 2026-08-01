package com.aigen.sims.agents;

/**
 * ReviewAgent — independent reviewer for the collective decision loop.
 * Votes APPROVE / REJECT / ABSTAIN with a confidence score (JSON).
 */
public class ReviewAgent extends SLMAgent {
    public ReviewAgent(String modelName) { super(modelName); }

    /** Independent review of a proposal. Returns JSON: {"vote":"APPROVE|REJECT|ABSTAIN","confidence":0.0-1.0,"reason":"..."} */
    public String review(String proposal) throws Exception {
        String prompt = "You are ReviewAgent, an independent reviewer in a collective of small models.\n"
                + "Review this proposal. Answer STRICTLY as JSON:\n"
                + "{\"vote\": \"APPROVE\" or \"REJECT\" or \"ABSTAIN\", \"confidence\": 0.0-1.0, \"reason\": \"one line\"}\n"
                + "Proposal: " + proposal;
        return generate(prompt, 200);
    }
}
