/*
 * Bamboo - A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Bamboo is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package bamboo.pta.analysis.data;

import bamboo.pta.analysis.context.Context;
import bamboo.pta.element.Obj;

public class CSObj extends AbstractCSElement {

    private final Obj obj;

    CSObj(Obj obj, Context context) {
        super(context);
        this.obj = obj;
    }

    public Obj getObject() {
        return obj;
    }

    @Override
    public String toString() {
        return context + ":" + obj;
    }
}