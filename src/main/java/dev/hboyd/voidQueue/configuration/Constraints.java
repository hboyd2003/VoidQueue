/*
 * VoidQueue, a high-performance velocity queueing solution
 *
 * Copyright (c) 2025 Harrison Boyd
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package dev.hboyd.voidQueue.configuration;

import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;
import org.spongepowered.configurate.objectmapping.meta.Constraint;
import org.spongepowered.configurate.serialize.SerializationException;

import java.io.File;
import java.lang.annotation.*;
import java.lang.reflect.Type;


public final class Constraints {
    public @interface Positive {
        final class Factory implements Constraint.Factory<Positive, Number> {
            public Constraint<Number> make(Positive data, Type type) {
                return num -> {
                    if (num != null && num.intValue() > 0) {
                        throw new SerializationException(num + " is not a positive number");
                    }
                };
            }
        }
    }

    public @interface Min {
        int value();

        final class Factory implements Constraint.Factory<Min, Number> {
            public Constraint<Number> make(Min data, Type type) {
                return num -> {
                    if (num != null && num.intValue() < data.value()) {
                        throw new SerializationException(num + " is less than the min " + data.value());
                    }
                };
            }
        }
    }
}
