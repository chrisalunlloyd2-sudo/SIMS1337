package com.aigen.sims.gui;
import java.util.*;
public class GuiGardenerTest {
    private static int passed = 0, failed = 0;

    public static void main(String[] args) {
        System.out.println("=== GUI Phase 5 Tests ===\n");
        testRegister();
        testApprove();
        testReject();
        testDeploy();
        testComponentMap();
        testPending();
        testComponentTypes();
        testGenerateCode();
        System.out.println("\n=== RESULTS: " + passed + " passed, " + failed + " failed ===");
        System.exit(failed > 0 ? 1 : 0);
    }

    static void check(String n, boolean c) {
        if (c) { passed++; System.out.println("  ✅ " + n); }
        else { failed++; System.out.println("  ❌ " + n + " FAILED"); }
    }

    static void testRegister() {
        System.out.println("testRegister:");
        GuiGardener g = new GuiGardener();
        String id = g.registerProposal(new ComponentProposal("button","MineBtn","}",
            "Button b=new Button(\"Mine\");","mine button","qwen2.5:0.5b",0,0));
        check("id not null", id != null);
        check("total = 1", g.getSummary().get("total") == 1);
    }

    static void testApprove() {
        System.out.println("\ntestApprove:");
        GuiGardener g = new GuiGardener();
        String id = g.registerProposal(new ComponentProposal("label","StatusLbl","}",
            "Label l=new Label(\"OK\");","status label","qwen2.5:0.5b",0,0));
        String code = g.approveProposal(id);
        check("code contains StatusLbl", code.contains("StatusLbl"));
        check("code contains END GUI", code.contains("END GUI"));
        check("approved = 1", g.getSummary().get("approved") == 1);
    }

    static void testReject() {
        System.out.println("\ntestReject:");
        GuiGardener g = new GuiGardener();
        String id = g.registerProposal(new ComponentProposal("button","BadBtn","}",
            "","bad","qwen2.5:0.5b",0,0));
        check("reject ok", g.rejectProposal(id));
        check("rejected = 1", g.getSummary().get("rejected") == 1);
    }

    static void testDeploy() {
        System.out.println("\ntestDeploy:");
        GuiGardener g = new GuiGardener();
        String id = g.registerProposal(new ComponentProposal("button","DeployBtn","}",
            "","deploy","qwen2.5:0.5b",0,0));
        g.approveProposal(id);
        check("mark deployed ok", g.markDeployed(id));
        check("deployed = 1", g.getSummary().get("deployed") == 1);
    }

    static void testComponentMap() {
        System.out.println("\ntestComponentMap:");
        GuiGardener g = new GuiGardener();
        g.registerProposal(new ComponentProposal("button","Btn1","}","","b1","qwen2.5:0.5b",0,0));
        g.registerProposal(new ComponentProposal("label","Lbl1","}","","l1","deepseek-r1:1.5b",0,0));
        String map = g.getComponentMapString();
        check("map contains Btn1", map.contains("Btn1"));
        check("map contains Lbl1", map.contains("Lbl1"));
    }

    static void testPending() {
        System.out.println("\ntestPending:");
        GuiGardener g = new GuiGardener();
        String id = g.registerProposal(new ComponentProposal("button","PendBtn","}",
            "","pending","qwen2.5:0.5b",0,0));
        check("1 pending", g.getPending().size() == 1);
        g.approveProposal(id);
        check("0 pending after approve", g.getPending().size() == 0);
    }

    static void testComponentTypes() {
        System.out.println("\ntestComponentTypes:");
        GuiGardener g = new GuiGardener();
        g.registerProposal(new ComponentProposal("button","Btn1","}","","b1","qwen2.5:0.5b",0,0));
        g.registerProposal(new ComponentProposal("label","Lbl1","}","","l1","deepseek-r1:1.5b",0,0));
        g.registerProposal(new ComponentProposal("chart","Chart1","}","","c1","phi:latest",0,0));
        Set<String> types = g.getComponentTypes();
        check("3 types", types.size() == 3);
        check("has button", types.contains("button"));
        check("has chart", types.contains("chart"));
    }

    static void testGenerateCode() {
        System.out.println("\ntestGenerateCode:");
        GuiGardener g = new GuiGardener();
        ComponentProposal p = new ComponentProposal("button","TestBtn","}",
            "        Button testBtn = new Button(\"Test\");\n        testBtn.setOnAction(e -> System.out.println(\"test\"));\n        vbox.getChildren().add(testBtn);",
            "test button","qwen2.5:0.5b",0,0);
        String code = g.generateInsertionCode(p);
        check("code contains TestBtn", code.contains("TestBtn"));
        check("code contains setOnAction", code.contains("testBtn.setOnAction"));
        check("code contains END GUI: TestBtn", code.contains("END GUI: TestBtn"));
    }
}
