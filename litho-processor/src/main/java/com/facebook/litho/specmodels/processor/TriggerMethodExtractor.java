/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.processor;

import static com.facebook.litho.specmodels.processor.MethodExtractorUtils.getMethodParams;
import static com.facebook.litho.specmodels.processor.MethodExtractorUtils.getTypeVariables;

import com.facebook.litho.annotations.FromTrigger;
import com.facebook.litho.annotations.OnTrigger;
import com.facebook.litho.annotations.Param;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.State;
import com.facebook.litho.annotations.TreeProp;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.EventDeclarationModel;
import com.facebook.litho.specmodels.model.EventMethodModel;
import com.facebook.litho.specmodels.model.MethodParamModel;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.Elements;

/** Extracts onTrigger methods from the given input. */
public class TriggerMethodExtractor {

  private static final List<Class<? extends Annotation>> METHOD_PARAM_ANNOTATIONS =
      new ArrayList<>();

  static {
    METHOD_PARAM_ANNOTATIONS.add(FromTrigger.class);
    METHOD_PARAM_ANNOTATIONS.add(Param.class);
    METHOD_PARAM_ANNOTATIONS.add(Prop.class);
    METHOD_PARAM_ANNOTATIONS.add(State.class);
    METHOD_PARAM_ANNOTATIONS.add(TreeProp.class);
  }

  /** Get the delegate methods from the given {@link TypeElement}. */
  public static ImmutableList<EventMethodModel> getOnTriggerMethods(
      Elements elements,
      TypeElement typeElement,
      List<Class<? extends Annotation>> permittedInterStageInputAnnotations) {
    final List<EventMethodModel> delegateMethods = new ArrayList<>();

    for (Element enclosedElement : typeElement.getEnclosedElements()) {
      if (enclosedElement.getKind() != ElementKind.METHOD) {
        continue;
      }

      final OnTrigger onTriggerAnnotation = enclosedElement.getAnnotation(OnTrigger.class);
      if (onTriggerAnnotation != null) {
        final ExecutableElement executableElement = (ExecutableElement) enclosedElement;

        final List<MethodParamModel> methodParams =
            getMethodParams(
                executableElement,
                getPermittedMethodParamAnnotations(permittedInterStageInputAnnotations),
                permittedInterStageInputAnnotations,
                ImmutableList.<Class<? extends Annotation>>of());

        final DeclaredType eventClassDeclaredType =
            ProcessorUtils.getAnnotationParameter(
                elements, executableElement, OnTrigger.class, "value");
        final Element eventClass = eventClassDeclaredType.asElement();

        // Reuse EventMethodModel and EventDeclarationModel because we are capturing the same info
        final EventMethodModel eventMethod =
            new EventMethodModel(
                new EventDeclarationModel(
                    ClassName.bestGuess(eventClass.toString()),
                    EventDeclarationsExtractor.getReturnType(elements, eventClass),
                    EventDeclarationsExtractor.getFields(eventClass),
                    eventClass),
                ImmutableList.copyOf(new ArrayList<>(executableElement.getModifiers())),
                executableElement.getSimpleName(),
                TypeName.get(executableElement.getReturnType()),
                ImmutableList.copyOf(getTypeVariables(executableElement)),
                ImmutableList.copyOf(methodParams),
                executableElement);
        delegateMethods.add(eventMethod);
      }
    }

    return ImmutableList.copyOf(delegateMethods);
  }

  private static List<Class<? extends Annotation>> getPermittedMethodParamAnnotations(
      List<Class<? extends Annotation>> permittedInterStageInputAnnotations) {
    final List<Class<? extends Annotation>> permittedMethodParamAnnotations =
        new ArrayList<>(METHOD_PARAM_ANNOTATIONS);
    permittedMethodParamAnnotations.addAll(permittedInterStageInputAnnotations);

    return permittedMethodParamAnnotations;
  }
}
