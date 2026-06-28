package com.serverdoctor.example;

import com.serverdoctor.api.module.AnalysisModule;
import com.serverdoctor.api.module.AnalysisResult;
import com.serverdoctor.api.module.ServerContext;
import com.serverdoctor.common.model.Finding;
import com.serverdoctor.common.model.Severity;

/**
 * A tiny custom scanner registered through the public API. It runs on every platform (no required
 * capabilities) and simply reports how many plugins are installed — demonstrating how a third-party
 * plugin can contribute its own findings to a ServerDoctor run.
 */
public final class ExampleScanner implements AnalysisModule {

    @Override
    public String id() {
        return "example";
    }

    @Override
    public AnalysisResult analyze(ServerContext ctx) {
        int plugins = ctx.plugins().size();
        Severity severity = plugins > 100 ? Severity.LOW : Severity.OK;
        return AnalysisResult.builder(id())
                .finding(new Finding(id(), severity, plugins + " plugins installed"))
                .build();
    }
}
