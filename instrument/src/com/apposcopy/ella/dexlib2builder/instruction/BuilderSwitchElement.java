package com.apposcopy.ella.dexlib2builder.instruction;

import com.apposcopy.ella.dexlib2builder.BuilderSwitchPayload;
import com.apposcopy.ella.dexlib2builder.Label;
import org.jf.dexlib2.iface.instruction.SwitchElement;

import javax.annotation.Nonnull;

public class BuilderSwitchElement implements SwitchElement {
    @Nonnull BuilderSwitchPayload parent;
    private final int key;
    @Nonnull private final Label target;

    public BuilderSwitchElement(@Nonnull BuilderSwitchPayload parent,
                                int key,
                                @Nonnull Label target) {
        this.parent = parent;
        this.key = key;
        this.target = target;
    }

    @Override public int getKey() {
        return key;
    }

    @Override public int getOffset() {
        return target.getCodeAddress() - parent.getReferrer().getCodeAddress();
    }

    @Nonnull
    public Label getTarget() {
        return target;
    }
}
