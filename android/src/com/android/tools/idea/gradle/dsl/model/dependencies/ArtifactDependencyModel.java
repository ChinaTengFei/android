/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.tools.idea.gradle.dsl.model.dependencies;

import com.android.tools.idea.gradle.dsl.dependencies.ExternalDependencySpec;
import com.android.tools.idea.gradle.dsl.parser.elements.*;
import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A Gradle artifact dependency. There are two notations supported for declaring a dependency on an external module. One is a string
 * notation formatted this way:
 * <pre>
 * configurationName "group:name:version:classifier@extension"
 * </pre>
 * The other is a map notation:
 * <pre>
 * configurationName group: group:, name: name, version: version, classifier: classifier, ext: extension
 * </pre>
 * For more details, visit:
 * <ol>
 *  <li><a href="https://docs.gradle.org/2.4/userguide/dependency_management.html">Gradle Dependency Management</a></li>
 *  <li><a href="https://docs.gradle.org/current/dsl/org.gradle.api.artifacts.dsl.DependencyHandler.html">Gradle
 * DependencyHandler</a></li>
 * </ol>
 */
public abstract class ArtifactDependencyModel extends DependencyModel {
  @NotNull
  public abstract String name();

  @Nullable
  public abstract String group();

  @Nullable
  public abstract String version();

  public abstract void setVersion(@NotNull String name);

  @Nullable
  public abstract String classifier();

  @Nullable
  public abstract String extension();

  @NotNull
  public String getCompactNotation() {
    return new ExternalDependencySpec(name(), group(), version(), classifier(), extension()).compactNotation();
  }

  @NotNull
  protected static List<ArtifactDependencyModel> create(@NotNull GradleDslElement element) {
    List<ArtifactDependencyModel> results = Lists.newArrayList();
    assert element instanceof GradleDslExpression || element instanceof GradleDslExpressionMap;
    if (element instanceof GradleDslExpressionMap) {
      results.add(new MapNotation((GradleDslExpressionMap)element));
    }
    else if (element instanceof GradleDslMethodCall) {
      if (!"project".equals(element.getName())) {
        for (GradleDslElement argument : ((GradleDslMethodCall)element).getArguments()) {
          results.addAll(create(argument));
        }
      }
    } else {
      GradleDslExpression dslLiteral = (GradleDslLiteral)element;
      String value = dslLiteral.getValue(String.class);
      if (value != null) {
        ExternalDependencySpec spec = ExternalDependencySpec.create(value);
        if (spec != null) {
          results.add(new CompactNotationModel((GradleDslLiteral)element, spec));
        }
      }
    }
    return results;
  }

  private static class MapNotation extends ArtifactDependencyModel {
    @NotNull private GradleDslExpressionMap myDslElement;

    MapNotation(@NotNull GradleDslExpressionMap dslElement) {
      myDslElement = dslElement;
    }

    @NotNull
    @Override
    public String name() {
      String value = myDslElement.getProperty("name", String.class);
      assert value != null;
      return value;
    }

    @Nullable
    @Override
    public String group() {
      return myDslElement.getProperty("group", String.class);
    }

    @Nullable
    @Override
    public String version() {
      return myDslElement.getProperty("version", String.class);
    }

    @Override
    public void setVersion(@NotNull String name) {
      myDslElement.setNewLiteral("version", name);
    }

    @Nullable
    @Override
    public String classifier() {
      return myDslElement.getProperty("classifier", String.class);
    }


    @Nullable
    @Override
    public String extension() {
      return myDslElement.getProperty("ext", String.class);
    }

    @Override
    @NotNull
    protected GradleDslElement getDslElement() {
      return myDslElement;
    }
  }

  private static class CompactNotationModel extends ArtifactDependencyModel {
    @NotNull private GradleDslExpression myDslExpression;
    @NotNull private ExternalDependencySpec mySpec;

    private CompactNotationModel(@NotNull GradleDslExpression dslExpression, @NotNull  ExternalDependencySpec spec) {
      myDslExpression = dslExpression;
      mySpec = spec;
    }

    @NotNull
    @Override
    public String name() {
      return mySpec.name;
    }

    @Nullable
    @Override
    public String group() {
      return mySpec.group;
    }

    @Nullable
    @Override
    public String version() {
      return mySpec.version;
    }

    @Override
    public void setVersion(@NotNull String version) {
      mySpec.version = version;
      myDslExpression.setValue(mySpec.toString());
    }

    @Nullable
    @Override
    public String classifier() {
      return mySpec.classifier;
    }

    @Nullable
    @Override
    public String extension() {
      return mySpec.extension;
    }

    @Override
    @NotNull
    protected GradleDslElement getDslElement() {
      return myDslExpression;
    }
  }
}