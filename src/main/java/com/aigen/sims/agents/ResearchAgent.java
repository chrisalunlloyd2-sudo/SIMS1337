package com.aigen.sims.agents;

/**
 * ResearchAgent — gathers context and summarizes for decision inputs.
 */
public class ResearchAgent extends SLMAgent {
    public ResearchAgent(String modelName) { super(modelName); }

    /** Summarize a body of text into decision-relevant facts. */
    public String research(String topic, String material) throws Exception {
        String prompt = "You are ResearchAgent. Given the material below, extract the facts "
                + "most relevant to the topic. Be concise, factual, no opinion.\n"
                + "Topic: " + topic + "\nMaterial:\n" + material;
        return generate(prompt, 400);
    }

    /** Generate sub-questions to decompose a complex decision (Markov chain step 1). */
    public String decompose(String decision) throws Exception {
        String prompt = "You are ResearchAgent. Decompose this complex decision into 3-4 verifiable "
                + "sub-claims, each of which a small model could confirm TRUE or FALSE independently.\n"
                + "Decision: " + decision + "\n"
                + "Format: one sub-claim per line, starting with '- '.";
        return generate(prompt, 300);
    }
}
