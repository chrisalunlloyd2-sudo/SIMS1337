package com.aigen.sims.agents;

/**
 * CodeAgent — generates code via local SLM.
 * Part of the WORLD_AS_DESKTOP paradigm: agents are files, tasks are files.
 */
public class CodeAgent extends SLMAgent {
    public CodeAgent(String modelName) { super(modelName); }

    /** Generate code for a task. Returns raw model output. */
    public String generateCode(String taskDescription, String language) throws Exception {
        String prompt = "You are CodeAgent. Write production-quality " + language
                + " code for the following task. Include comments and error handling.\n"
                + "Task: " + taskDescription + "\n"
                + "Respond with ONLY the code block.";
        return generate(prompt, 600);
    }

    /** Review a code snippet for bugs/improvements. Returns critique. */
    public String reviewCode(String code, String language) throws Exception {
        String prompt = "You are CodeAgent reviewing " + language + " code.\n"
                + "Find bugs, security issues, and improvements. Be specific.\n"
                + "Code:\n" + code;
        return generate(prompt, 400);
    }
}
