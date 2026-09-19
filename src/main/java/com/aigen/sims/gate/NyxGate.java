package com.aigen.sims.gate;

import com.aigen.sims.deploy.DeployProposal;
import com.aigen.sims.deploy.GateKeeper;

/**
 * NyxGate -- 2026-07-28 Architect gist 3.3: "Nyx currently does probabilistic safety checks. Add
 * symbolic AST verification ... only pass to deploy/ if the symbolic check passes."
 *
 * Wraps GateKeeper.approveProposal() with a real symbolic bracket check (AlgebraicCorrector) BEFORE
 * anything gets written/committed. On imbalance, retries repair up to 3 times; on success tags
 * [AUTONOMOUSLY REPAIRED], on failure rejects the proposal and tags [BLOCKED -- HUMAN REQUIRED].
 * Never bypasses GateKeeper's own quorum/human gate levels -- it only adds a check BEFORE them.
 */
public class NyxGate {

    public static class NyxResult {
        public final boolean allowed;
        public final String tag;
        public final String detail;

        public NyxResult(boolean allowed, String tag, String detail) {
            this.allowed = allowed;
            this.tag = tag;
            this.detail = detail;
        }

        @Override
        public String toString() { return tag + " " + detail; }
    }

    public static NyxResult verifyAndApprove(GateKeeper gk, String proposalId) throws Exception {
        DeployProposal p = gk.getProposal(proposalId);
        if (p == null) {
            return new NyxResult(false, "[BLOCKED -- HUMAN REQUIRED]", "no such proposal: " + proposalId);
        }

        AlgebraicCorrector.ScanResult scan = AlgebraicCorrector.scan(p.newContent);
        boolean repairedAny = false;
        int attempt = 0;
        while (!scan.balanced && scan.repairable && attempt < 3) {
            attempt++;
            gk.updateProposalContent(proposalId, scan.repaired);
            repairedAny = true;
            p = gk.getProposal(proposalId);
            scan = AlgebraicCorrector.scan(p.newContent);
        }

        if (!scan.balanced) {
            gk.rejectProposal(proposalId);
            return new NyxResult(false, "[BLOCKED -- HUMAN REQUIRED]",
                "symbolic AST check failed after " + attempt + " repair attempt(s): " + scan.issues);
        }

        String mergeResult = gk.approveProposal(proposalId);
        return new NyxResult(true, repairedAny ? "[AUTONOMOUSLY REPAIRED]" : "[VERIFIED]", mergeResult);
    }
}
