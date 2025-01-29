/*
 * author     Adrian <adrian@enspi.red>
 * copyright  2025
 * license    GPL-3.0 (only)
 *
 *  This program is free software: you can redistribute it and/or modify it
 *  under the terms of the GNU General public final License, version 3.
 *  The right to apply the terms of later versions of the GPL is RESERVED.
 *
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU General public final License for more details.
 *
 *  You should have received a copy of the GNU General public final License along with this program.
 *  If not, see <http://www.gnu.org/licenses/gpl-3.0.txt>.
 */
package red.enspi.beanerator;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.SourceVersion;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;

@SupportedAnnotationTypes("red.enspi.beanerator.Beanerate")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class Beanerator extends AbstractProcessor {

  private static final String TPL_CLASS = """
    /** This is an auto-generated class file. */
    package {package};
    import java.util.Objects;

    /** A basic bean interoperable with {@code {recordname}} records. */
    {annotations}{visibility} final class {classname}{interfaces} {

    {fromRecord}

    {constructors}

    {equals}

    {toRecord}

    {toString}

    {fieldAccessors}}
    """;

  private static final String TPL_FROMRECORD = """
      /** Builds a bean from the given {@code {recordname}} record. */
      public static {classname} fromRecord({recordname} record) {
        return new {classname}({values});
      }
    """;

  private static final String TPL_CONSTRUCTORS = """
      /** Null constructor. */
      public {classname}() {}

      /** Canonical constructor. */
      public {classname}({args}) {
    {assignments}
      }
    """;

  private static final String TPL_ACCESSORS = """
      private {type} {field} = null;

      /** Gets the value of {@code {field}}. */
      public {type} get{Field}() {
        return this.{field};
      }

      /** Sets the value of {@code {field}}. */
      public {classname} set{Field}({type} {field}) {
        this.{field} = {field};
        return this;
      }
    """;

  private static final String TPL_EQUALS = """
      @Override
      public boolean equals(Object other) {
        return this == other || (
          other instanceof {classname} other{classname}
    {comparisons}
        );
      }

      @Override
      public int hashCode() {
        return Objects.hash({fields});
      }
    """;

  private static final String TPL_TORECORD = """
      /** Builds a {@code {recordname}} record from this bean. */
      public {recordname} toRecord() {
        return new {recordname}({values});
      }
    """;

  private static final String TPL_TOSTRING = """
      @Override
      public String toString() {
        return "{classname}["
    {fieldsToString}
          + "]";
      }
    """;

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    for (var element : roundEnv.getElementsAnnotatedWith(Beanerate.class)) {
      if (element instanceof TypeElement typeElement && typeElement.getKind() == ElementKind.RECORD) {
        try {
          this.generate(typeElement);
        } catch (Exception e) {
          this.processingEnv.getMessager().printMessage(
            Diagnostic.Kind.ERROR,
            "Failed to beanerate `%s`: %s".formatted(typeElement.getQualifiedName(), e));
        }
      }
    }

    return false; // Continue processing other annotations
  }

  private String findPackage(Element record) {
    var element = record;
    while (element.getEnclosingElement() instanceof Element enclosingElement) {
      if (enclosingElement instanceof PackageElement packageElement) {
        return packageElement.getQualifiedName().toString();
      }
      element = enclosingElement;
    }
    return "red.enspi.beanerator";
  }

  private String findVisibility(TypeElement record) {
    var modifiers = record.getModifiers();
    if (modifiers.contains(Modifier.PUBLIC)) {
      return "public";
    }
    if (modifiers.contains(Modifier.PROTECTED)) {
      return "protected";
    }
    if (modifiers.contains(Modifier.PRIVATE)) {
      return "private";
    }
    return ""; // package-private
  }

  private void generate(TypeElement record) {
    var messager = this.processingEnv.getMessager();
    messager.printMessage(
      Diagnostic.Kind.NOTE,
      "Generating bean for %s...".formatted(record.getQualifiedName()),
      record);

    var annotation = record.getAnnotation(Beanerate.class);
    var packageName = this.findPackage(record);
    var recordName = record.getQualifiedName().toString();
    var className = record.getSimpleName().toString() + "Bean";
    List<Field> fields = new ArrayList<>();
    for (var component : record.getRecordComponents()) {
      fields.add(new Field(component, this.processingEnv));
    }

    var source = Beanerator.TPL_CLASS
      .replace("{annotations}", this.generateAnnotations(annotation))
      .replace("{visibility}", this.findVisibility(record))
      .replace("{interfaces}", this.generateInterfaces(annotation))
      .replace("{fromRecord}", this.generateFromRecord(fields))
      .replace("{constructors}", this.generateConstructors(fields))
      .replace("{equals}", this.generateEquals(fields))
      .replace("{fieldAccessors}", this.generateFieldAccessors(fields))
      .replace("{toRecord}", this.generateToRecord(fields))
      .replace("{toString}", this.generateToString(fields))
      // these replacements are last on purpose
      .replace("{package}", packageName)
      .replace("{recordname}", recordName)
      .replace("{classname}", className);


    try (
      Writer writer = processingEnv.getFiler()
        .createSourceFile(packageName + "." + className)
        .openWriter()
    ) {
      writer.write(source);
    } catch (IOException e) {
      messager.printMessage(
        Diagnostic.Kind.ERROR,
        "Code generation failed: %s".formatted(e.getMessage()),
        record);
    }
  }

  private String generateAnnotations(Beanerate annotation) {
    var annotations = new ArrayList<String>();
    for (var a : annotation.annotations()) {
      annotations.add("@" + a + "\n");
    }
    return String.join("", annotations);
  }

  private String generateInterfaces(Beanerate annotation) {
    var interfaces = annotation.interfaces();
    return (interfaces.length == 0) ?
      "" :
      " implements " + String.join(", ", interfaces);
  }

  private String generateFieldAccessors(List<Field> fields) {
    var all = new ArrayList<String>();
    for (var field : fields) {
      all.add(
        Beanerator.TPL_ACCESSORS
          .replace("{field}", field.name())
          .replace("{Field}", field.capName())
          .replace("{type}", field.beaneratedType()));
    }
    return String.join("\n\n", all);
  }

  private String generateConstructors(List<Field> fields) {
    var args = new ArrayList<String>();
    var assignments = new ArrayList<String>();
    for (var field: fields) {
      var name = field.name();
      args.add(field.beaneratedType() + " " + name);
      assignments.add("    this." + name + " = " + name + ";");
    }
    return Beanerator.TPL_CONSTRUCTORS
      .replace("{args}", String.join(", ", args))
      .replace("{assignments}", String.join("\n", assignments));
  }

  private String generateEquals(List<Field> fields) {
    var properties = new ArrayList<String>();
    var comparisons = new ArrayList<String>();
    for (var field: fields) {
      var name = field.name();
      properties.add("this." + name);
      comparisons.add("        && this." + name + " == other{classname}.get" + field.capName() + "()");
    }
    return Beanerator.TPL_EQUALS
      .replace("{comparisons}", String.join("\n", comparisons))
      .replace("{fields}", String.join(", ", properties));
  }

  private String generateFromRecord(List<Field> fields) {
    var values = new ArrayList<String>();
    for (var field : fields) {
      values.add(
        field.isBeanerated() ?
          field.beaneratedType() + ".fromRecord(record." + field.name() + "())" :
          "record." + field.name() + "()"
      );
    }
    return Beanerator.TPL_FROMRECORD.replace("{values}", String.join(", ", values));
  }

  private String generateToRecord(List<Field> fields) {
    var values = new ArrayList<String>();
    for (var field : fields) {
      values.add("this.get" + field.capName() + (field.isBeanerated() ? "().toRecord()" : "()"));
    }
    return Beanerator.TPL_TORECORD.replace("{values}", String.join(", ", values));
  }

  private String generateToString(List<Field> fields) {
    var dumps = new ArrayList<String>();
    for (var field : fields) {
      var name = field.name();
      dumps.add("      + \"" + name + "=\" + this." + name);
    }
    return Beanerator.TPL_TOSTRING.replace("{fieldsToString}", String.join(" + \", \"\n", dumps));
  }

  record Field(String name, TypeMirror type, boolean isBeanerated) {

    public Field(RecordComponentElement component, ProcessingEnvironment env) {
      this(
        component.getSimpleName().toString(),
        component.asType(),
        (
          component.asType() instanceof DeclaredType type
          && env.getTypeUtils().asElement(type) instanceof TypeElement typeElement
          && typeElement.getKind() == ElementKind.RECORD
          && typeElement.getAnnotation(Beanerate.class) instanceof Beanerate
        ));
    }

    public String beaneratedType() {
      return this.isBeanerated ? (this.nativeType() + "Bean") : this.nativeType();
    }

    public String capName() {
      return this.name.substring(0, 1).toUpperCase() + this.name.substring(1).toLowerCase();
    }

    public String nativeType() {
      return this.type.toString();
    }
  }
}
