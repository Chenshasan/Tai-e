/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2020-- Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020-- Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * Tai-e is only for educational and academic purposes,
 * and any form of commercial use is disallowed.
 * Distribution of Tai-e is disallowed without the approval.
 */

package pascal.taie.analysis.oldpta.core.solver;

import pascal.taie.World;
import pascal.taie.analysis.oldpta.PTAOptions;
import pascal.taie.analysis.oldpta.core.context.ContextInsensitiveSelector;
import pascal.taie.analysis.oldpta.core.context.OneCallSelector;
import pascal.taie.analysis.oldpta.core.context.OneObjectSelector;
import pascal.taie.analysis.oldpta.core.context.OneTypeSelector;
import pascal.taie.analysis.oldpta.core.context.TwoCallSelector;
import pascal.taie.analysis.oldpta.core.context.TwoObjectSelector;
import pascal.taie.analysis.oldpta.core.context.TwoTypeSelector;
import pascal.taie.analysis.oldpta.core.cs.MapBasedCSManager;
import pascal.taie.analysis.oldpta.core.heap.AllocationSiteBasedModel;
import pascal.taie.analysis.oldpta.plugin.AnalysisTimer;
import pascal.taie.analysis.oldpta.plugin.CompositePlugin;
import pascal.taie.analysis.oldpta.plugin.Preprocessor;
import pascal.taie.analysis.oldpta.plugin.ResultPrinter;
import pascal.taie.analysis.oldpta.plugin.ThreadHandler;
import pascal.taie.analysis.oldpta.set.HybridPointsToSet;
import pascal.taie.analysis.oldpta.set.PointsToSetFactory;
import pascal.taie.util.AnalysisException;

public class PointerAnalysisBuilder {

    public PointerAnalysis build(PTAOptions PTAOptions) {
        PointsToSetFactory.setFactory(new HybridPointsToSet.Factory());
        PointerAnalysisImpl pta = new PointerAnalysisImpl();
        setContextSensitivity(pta, PTAOptions);
        setPlugin(pta);
        pta.setHeapModel(new AllocationSiteBasedModel(
                World.getTypeManager()));
        pta.setCSManager(new MapBasedCSManager());
        return pta;
    }

    private void setContextSensitivity(PointerAnalysisImpl pta, PTAOptions PTAOptions) {
        switch (PTAOptions.getContextSensitivity()) {
            case "ci":
                pta.setContextSelector(new ContextInsensitiveSelector());
                break;
            case "1-call":
            case "1-cfa":
                pta.setContextSelector(new OneCallSelector());
                break;
            case "1-obj":
            case "1-object":
                pta.setContextSelector(new OneObjectSelector());
                break;
            case "1-type":
                pta.setContextSelector(new OneTypeSelector());
                break;
            case "2-call":
            case "2-cfa":
                pta.setContextSelector(new TwoCallSelector());
                break;
            case "2-obj":
            case "2-object":
                pta.setContextSelector(new TwoObjectSelector());
                break;
            case "2-type":
                pta.setContextSelector(new TwoTypeSelector());
                break;
            default:
                throw new AnalysisException(
                        "Unknown context sensitivity variant: "
                                + PTAOptions.getContextSensitivity());
        }
    }

    private void setPlugin(PointerAnalysisImpl pta) {
        CompositePlugin plugin = new CompositePlugin();
        // To record elapsed time precisely, AnalysisTimer should be
        // added at first.
        // TODO: remove such order dependency
        plugin.addPlugin(
                new AnalysisTimer(),
                new Preprocessor(),
                new ThreadHandler(),
                ResultPrinter.v()
        );
        plugin.setPointerAnalysis(pta);
        pta.setPlugin(plugin);
    }
}