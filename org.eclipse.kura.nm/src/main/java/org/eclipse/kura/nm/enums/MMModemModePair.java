/*******************************************************************************
 * Copyright (c) 2026 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.nm.enums;

import java.util.Objects;
import java.util.Set;

/**
 * This class represents a pair of MMModemManager modes and the preferred one.
 *
 */
public class MMModemModePair {

    private final Set<MMModemMode> modes;
    private final MMModemMode preferredMode;

    public MMModemModePair(Set<MMModemMode> modes, MMModemMode preferredMode) {
        this.modes = modes;
        this.preferredMode = preferredMode;
    }

    public Set<MMModemMode> getModes() {
        return this.modes;
    }

    public MMModemMode getPreferredMode() {
        return this.preferredMode;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.modes, this.preferredMode);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        MMModemModePair other = (MMModemModePair) obj;
        return Objects.equals(this.modes, other.modes) && this.preferredMode == other.preferredMode;
    }

    @Override
    public String toString() {
        return String.format("MMModemModePair [modes=%s, preferredMode=%s]", this.modes, this.preferredMode);
    }

}
