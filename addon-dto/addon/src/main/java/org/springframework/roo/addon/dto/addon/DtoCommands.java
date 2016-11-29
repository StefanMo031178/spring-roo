package org.springframework.roo.addon.dto.addon;

import static org.springframework.roo.shell.OptionContexts.PROJECT;
import static org.springframework.roo.shell.OptionContexts.UPDATELAST_PROJECT;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.field.addon.FieldCommands;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.classpath.scanner.MemberDetailsScanner;
import org.springframework.roo.converters.LastUsed;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.JpaJavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CliOptionAutocompleteIndicator;
import org.springframework.roo.shell.CliOptionMandatoryIndicator;
import org.springframework.roo.shell.CliOptionVisibilityIndicator;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.roo.shell.Converter;
import org.springframework.roo.shell.ShellContext;
import org.springframework.roo.support.logging.HandlerUtils;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Commands for the DTO add-on to be used by the ROO shell.
 *
 * @author Sergio Clares
 * @since 2.0
 */
@Component
@Service
public class DtoCommands implements CommandMarker {

  protected final static Logger LOGGER = HandlerUtils.getLogger(FieldCommands.class);

  // ------------ OSGi component attributes ----------------
  private BundleContext context;

  @Reference
  private DtoOperations dtoOperations;

  @Reference
  private TypeLocationService typeLocationService;

  @Reference
  private ProjectOperations projectOperations;

  @Reference
  private PathResolver pathResolver;

  @Reference
  private FileManager fileManager;

  @Reference
  private LastUsed lastUsed;

  @Reference
  private MemberDetailsScanner memberDetailsScanner;

  private Converter<JavaType> javaTypeConverter;

  protected void activate(final ComponentContext context) {
    this.context = context.getBundleContext();
  }

  protected void deactivate(final ComponentContext context) {
    this.context = null;
  }

  @CliAvailabilityIndicator({"dto"})
  public boolean isDtoCreationAvailable() {
    return dtoOperations.isDtoCreationPossible();
  }

  @CliAvailabilityIndicator({"entity projection"})
  public boolean isEntityProjectionAvailable() {
    return dtoOperations.isEntityProjectionPossible();
  }

  @CliCommand(
      value = "dto",
      help = "Creates a new DTO (Data Transfer Object) class in the directory _src/main/java_ of the selected project module (if any) with @RooDTO annotation.")
  public void newDtoClass(
      @CliOption(
          key = "class",
          mandatory = true,
          optionContext = UPDATELAST_PROJECT,
          help = "The name of the DTO class to create. If you consider it necessary, "
              + "you can also specify the package (base package can be specified with `~`). "
              + "Ex.: `--class ~.domain.MyDto`. You can specify module as well, if necessary. "
              + "Ex.: `--class model:~.domain.MyDto`. When working with a multi-module project, "
              + "if module is not specified the class will be created in the module which has the focus.") final JavaType name,
      @CliOption(key = "immutable", mandatory = false, specifiedDefaultValue = "true",
          unspecifiedDefaultValue = "false", help = "Whether the DTO should be inmutable. "
              + "Default if option present: `true`; default if option not present: `false`.") final boolean immutable,
      @CliOption(
          key = "utilityMethods",
          mandatory = false,
          specifiedDefaultValue = "true",
          unspecifiedDefaultValue = "false",
          help = "Whether the DTO should implement `toString()`, `hashCode()` and `equals()` methods. "
              + "Default if option present: `true`; default if option not present: `false`.") final boolean utilityMethods,
      @CliOption(key = "serializable", mandatory = false, specifiedDefaultValue = "true",
          unspecifiedDefaultValue = "false",
          help = "Whether the DTO should implement `java.io.Serializable`. "
              + "Default if option present: `true`; default if option not present: `false`.") final boolean serializable,
      ShellContext shellContext) {

    // Check if DTO already exists
    final String entityFilePathIdentifier =
        pathResolver.getCanonicalPath(name.getModule(), Path.SRC_MAIN_JAVA, name);
    if (fileManager.exists(entityFilePathIdentifier) && shellContext.isForce()) {
      fileManager.delete(entityFilePathIdentifier);
    } else if (fileManager.exists(entityFilePathIdentifier) && !shellContext.isForce()) {
      throw new IllegalArgumentException(
          String
              .format(
                  "DTO '%s' already exists and cannot be created. Try to use a "
                      + "different DTO name on --class parameter or use --force parameter to overwrite it.",
                  name));
    }

    dtoOperations.createDto(name, immutable, utilityMethods, serializable);

  }

  /**
   * Makes 'entity' option mandatory if 'class' has been defined.
   *
   * @param shellContext
   * @return true if 'class' has been defined, false otherwise.
   */
  @CliOptionMandatoryIndicator(command = "entity projection", params = {"entity"})
  public boolean isEntityMandatoryForEntityProjection(ShellContext shellContext) {

    // Check already specified params
    Map<String, String> params = shellContext.getParameters();
    if (params.containsKey("class")) {
      return true;
    }

    return false;
  }

  /**
   * Makes 'fields' option mandatory if 'entity' has been defined.
   *
   * @param shellContext
   * @return true if 'entity' has been defined, false otherwise.
   */
  @CliOptionMandatoryIndicator(command = "entity projection", params = {"fields"})
  public boolean isFieldsMandatoryForEntityProjection(ShellContext shellContext) {

    // Check already specified params
    Map<String, String> params = shellContext.getParameters();
    if (params.containsKey("entity")) {
      return true;
    }

    return false;
  }

  /**
   * Makes 'all' option visible only if 'class' option is not specified.
   *
   * @param shellContext
   * @return false if 'class' is specified, true otherwise.
   */
  @CliOptionVisibilityIndicator(command = "entity projection", params = {"all"},
      help = "Option 'all' can't be used with 'class' option in the same command.")
  public boolean isAllVisibleForEntityProjection(ShellContext shellContext) {

    // Check already specified params
    Map<String, String> params = shellContext.getParameters();
    if (params.containsKey("class")) {
      return false;
    }

    return true;
  }

  /**
   * Makes 'class' option visible only if 'all' option is not specified.
   *
   * @param shellContext
   * @return false if 'all' is specified, true otherwise.
   */
  @CliOptionVisibilityIndicator(command = "entity projection", params = {"class"},
      help = "Option 'class' can't be used with 'all' option in the same command.")
  public boolean isClassVisibleForEntityProjection(ShellContext shellContext) {

    // Check already specified params
    Map<String, String> params = shellContext.getParameters();
    if (params.containsKey("all")) {
      return false;
    }

    return true;
  }

  /**
   * Makes 'entity' option visible only if 'class' option is already specified.
   *
   * @param shellContext
   * @return true if 'class' is specified, false otherwise.
   */
  @CliOptionVisibilityIndicator(command = "entity projection", params = {"entity"},
      help = "Option 'entity' can't be used until 'class' option is specified.")
  public boolean isEntityVisibleForEntityProjection(ShellContext shellContext) {

    // Check already specified params
    Map<String, String> params = shellContext.getParameters();
    if (params.containsKey("class")) {
      return true;
    }

    return false;
  }

  /**
   * Makes 'suffix' option visible only if 'all' option is specified.
   *
   * @param shellContext
   * @return true if 'all' is specified, false otherwise.
   */
  @CliOptionVisibilityIndicator(command = "entity projection", params = {"suffix"},
      help = "Option 'suffix' can't be used if option 'all' isn't already specified.")
  public boolean isSuffixVisibleForEntityProjection(ShellContext shellContext) {

    // Check already specified params
    Map<String, String> params = shellContext.getParameters();
    if (params.containsKey("all")) {
      return true;
    }

    return false;
  }

  /**
   * Makes 'fields' option visible only if 'entity' option is specified.
   *
   * @param shellContext
   * @return true if 'entity' is specified, false otherwise.
   */
  @CliOptionVisibilityIndicator(command = "entity projection", params = {"fields"},
      help = "Option 'fields' can't be used if option 'entity' isn't already specified.")
  public boolean isFieldsVisibleForEntityProjection(ShellContext shellContext) {

    // Check already specified params
    Map<String, String> params = shellContext.getParameters();
    if (params.containsKey("entity")) {
      return true;
    }

    return false;
  }

  /**
   * Find entities in project and returns a list with their fully qualified
   * names.
   *
   * @param shellContext
   * @return List<String> with available entity full qualified names.
   */
  @CliOptionAutocompleteIndicator(command = "entity projection", param = "entity",
      help = "Option 'entity' must have an existing entity value. Please, assign it a right value.")
  public List<String> returnEntityValues(ShellContext shellContext) {

    // Get current value of class
    String currentText = shellContext.getParameters().get("entity");

    // Create results to return
    List<String> results = new ArrayList<String>();

    // Get entity fully qualified names
    Set<ClassOrInterfaceTypeDetails> entities =
        typeLocationService.findClassesOrInterfaceDetailsWithAnnotation(RooJavaType.ROO_JPA_ENTITY,
            JpaJavaType.ENTITY);
    for (ClassOrInterfaceTypeDetails entity : entities) {
      String name = replaceTopLevelPackageString(entity, currentText);
      if (!results.contains(name)) {
        results.add(name);
      }
    }

    return results;
  }

  /**
   * Attempts to obtain entity specified in 'entity' option and returns an
   * auto-complete list with the entity fields, separated by comma's.
   * 
   * @param shellContext
   * @return a List<String> with the possible values.
   */
  @CliOptionAutocompleteIndicator(command = "entity projection", param = "fields",
      help = "Option fields must have a comma-separated list of valid fields. Please, assign it a "
          + "correct value. Transient, static and entity collection fields are not valid for "
          + "projections.", includeSpaceOnFinish = false)
  public List<String> returnFieldValues(ShellContext shellContext) {
    List<String> fieldValuesToReturn = new ArrayList<String>();

    // Get entity JavaType
    JavaType entity = getTypeFromEntityParam(shellContext);

    // Get current fields in --field value
    String currentFieldValue = shellContext.getParameters().get("fields");
    String[] fields = StringUtils.split(currentFieldValue, ",");

    // Check for bad written separators and return no options
    if (currentFieldValue.contains(",.") || currentFieldValue.contains(".,")) {
      return fieldValuesToReturn;
    }

    // Check if it is first field
    if (currentFieldValue.equals("")) {
      for (FieldMetadata field : getEntityFieldList(entity)) {
        fieldValuesToReturn.add(field.getFieldName().getSymbolName());
      }
      return fieldValuesToReturn;
    }

    // VALIDATION OF CURRENT SPECIFIED VALUES UNTIL LAST MEMBER
    JavaType lastRelatedEntity = null;
    String completedValue = "";
    List<FieldMetadata> entityFields = null;
    boolean fieldFound = false;
    boolean lastFieldIsEntity = false;
    boolean isMainEntityField = true;
    for (int i = 0; i < fields.length; i++) {

      JavaType lastFieldType = entity;

      // Split field by ".", in case it was a relation field
      String[] splittedByPeriod = StringUtils.split(fields[i], ".");

      // Build auto-complete values
      for (int t = 0; t < splittedByPeriod.length; t++) {
        fieldFound = false;

        // Find the field in entity fields
        if (typeLocationService.getTypeDetails(lastFieldType) != null
            && typeLocationService.getTypeDetails(lastFieldType).getAnnotation(
                RooJavaType.ROO_JPA_ENTITY) != null) {
          entityFields = getEntityFieldList(lastFieldType);
        }

        for (FieldMetadata entityField : entityFields) {
          lastFieldIsEntity = false;
          if (splittedByPeriod[t].equals(entityField.getFieldName().getSymbolName())) {

            // Add auto-complete value
            if (completedValue.equals("")) {
              completedValue = entityField.getFieldName().getSymbolName();
            } else {
              if (splittedByPeriod.length > 1 && t > 0) {

                // Field is from a relation
                completedValue =
                    completedValue.concat(".").concat(entityField.getFieldName().getSymbolName());
              } else {

                // Field is a simple field
                completedValue =
                    completedValue.concat(",").concat(entityField.getFieldName().getSymbolName());
              }
            }

            // Record last field JavaType for auto-completing last value
            lastFieldType = entityField.getFieldType();

            // Check if field is an entity different from original entity
            if (typeLocationService.getTypeDetails(lastFieldType) != null
                && typeLocationService.getTypeDetails(lastFieldType).getAnnotation(
                    RooJavaType.ROO_JPA_ENTITY) != null
                && !entityField.getFieldType().equals(entity)) {
              lastFieldIsEntity = true;
              lastRelatedEntity = lastFieldType;
            }

            fieldFound = true;
            break;
          }
        }

        // Checks if field to autocomplete is from main entity
        if (currentFieldValue.endsWith(".") || (splittedByPeriod.length > 1 && !fieldFound)) {
          isMainEntityField = false;
        }
      }
    }

    // ADDITION OF NEW VALUES

    // Add field separator if needed
    if (fieldFound) {

      // Always add current value for validation only
      fieldValuesToReturn.add(completedValue);

      // If field is entity, append , and . and return values
      if (lastFieldIsEntity) {
        fieldValuesToReturn.add(completedValue.concat(","));
        fieldValuesToReturn.add(completedValue.concat("."));

        if (!currentFieldValue.endsWith(",") && !currentFieldValue.endsWith(".")) {
          return fieldValuesToReturn;
        }
      }
    }

    // Build auto-complete values for last member
    String autocompleteValue = "";
    if (isMainEntityField) {

      // Complete simple fields. Add entity fields as auto-complete values
      List<FieldMetadata> mainEntityFields = getEntityFieldList(entity);
      for (FieldMetadata mainEntityField : mainEntityFields) {

        if (completedValue.equals("")) {

          // Is first field to complete
          fieldValuesToReturn.add(mainEntityField.getFieldName().getSymbolName());
        } else if (!completedValue.equals("")) {

          // Check if field is specified and add it if not
          boolean alreadySpecified = false;
          // boolean relationField = false;
          for (int i = 0; i < fields.length; i++) {
            if (mainEntityField.getFieldName().getSymbolName().equals(fields[i])) {
              alreadySpecified = true;
            }
          }
          if (!alreadySpecified) {

            // Add completion
            autocompleteValue =
                completedValue.concat(",").concat(mainEntityField.getFieldName().getSymbolName());
          } else if (alreadySpecified && typeIsEntity(mainEntityField.getFieldType())) {

            // Add completion as relation field
            autocompleteValue =
                completedValue.concat(",").concat(mainEntityField.getFieldName().getSymbolName())
                    .concat(".");
          }

          // Add completion
          fieldValuesToReturn.add(autocompleteValue);
        }
      }
    } else if (lastRelatedEntity != null) {

      // Complete with fields of current relation field
      List<FieldMetadata> relatedEntityFields = getEntityFieldList(lastRelatedEntity);
      for (FieldMetadata relatedEntityField : relatedEntityFields) {
        autocompleteValue =
            completedValue.concat(".").concat(relatedEntityField.getFieldName().getSymbolName());

        // Check if value already exists
        String additionalValueToAdd = StringUtils.substringAfterLast(autocompleteValue, ",");
        if (!fieldValuesToReturn.contains(autocompleteValue)
            && !currentFieldValue.contains(additionalValueToAdd)) {
          fieldValuesToReturn.add(autocompleteValue);
        } else if (!fieldValuesToReturn.contains(autocompleteValue)
            && typeIsEntity(relatedEntityField.getFieldType())) {
          fieldValuesToReturn.add(autocompleteValue);
        } else if (additionalValueToAdd.equals("")) {
          fieldValuesToReturn.add(autocompleteValue);
        }
      }
    }

    return fieldValuesToReturn;
  }

  @CliCommand(
      value = "entity projection",
      help = "Creates new projection classes from entities in the directory _src/main/java_ of the "
          + "selected project module (if any) annotated with `@RooEntityProjection`. Transient, "
          + "static and entity collection fields are not valid for projections.")
  public void newProjectionClass(
      @CliOption(
          key = "all",
          mandatory = false,
          specifiedDefaultValue = "true",
          unspecifiedDefaultValue = "false",
          help = "Create one projection class for each entity in the project."
              + "This option is mandatory if `--class` is not specified. Otherwise, using `--class` will cause the parameter `--all` won't be available.") final boolean all,
      @CliOption(
          key = "class",
          mandatory = false,
          optionContext = UPDATELAST_PROJECT,
          help = "The name of the projection class to create. If you consider it necessary, "
              + "you can also specify the package (base package can be specified with `~`). "
              + "Ex.: `--class ~.domain.MyProjection`. You can specify module as well, if necessary. "
              + "Ex.: `--class model:~.domain.MyProjection`. When working with a multi-module "
              + "project, if module is not specified the projection will be created in the module "
              + "which has the focus."
              + "This option is mandatory if `--all` is not specified. Otherwise, using `--all` will cause the parameter `--class` won't be") final JavaType name,
      @CliOption(
          key = "entity",
          mandatory = true,
          help = "Name of the entity which can be used to create the Projection from. "
              + "This option is mandatory if `--class` is specified. Otherwise, not specifying `--class` will cause the parameter `--entity` won't be available.") final JavaType entity,
      @CliOption(
          key = "fields",
          mandatory = true,
          help = "Comma separated list of entity fields to be included into the Projection. "
              + "Possible values are: non-static, nor transient, nor entity collection fields from "
              + "main entity or its related entities (only for one-to-one or many-to-one relations). "
              + "This option is mandatory if `--class` is specified. Otherwise, not specifying "
              + "`--class` will cause the parameter `--fields` won't be available.") final String fields,
      @CliOption(
          key = "suffix",
          mandatory = false,
          unspecifiedDefaultValue = "Projection",
          help = "Suffix added to each Projection class name, built from each associated entity name. "
              + "This option is only available if `--all` has been already specified."
              + "Default if option not present: 'Projection'.") final String suffix,
      ShellContext shellContext) {

    // Check if Projection already exists
    if (name != null) {
      final String entityFilePathIdentifier =
          pathResolver.getCanonicalPath(name.getModule(), Path.SRC_MAIN_JAVA, name);
      if (fileManager.exists(entityFilePathIdentifier) && shellContext.isForce()) {
        fileManager.delete(entityFilePathIdentifier);
      } else if (fileManager.exists(entityFilePathIdentifier) && !shellContext.isForce()) {
        throw new IllegalArgumentException(
            String
                .format(
                    "Projection '%s' already exists and cannot be created. Try to use a "
                        + "different Projection name on --class parameter or use --force parameter to overwrite it.",
                    name));
      }
    }

    // Check if --fields has a value
    if (entity != null && StringUtils.isBlank(fields)) {
      throw new IllegalArgumentException(
          String
              .format(
                  "Projection '%s' should have at least one field from its associated entity. Please, add a right value for 'fields' option.",
                  name));
    }

    if (entity != null) {
      dtoOperations.createProjection(entity, name, fields, null);
    } else if (all == true) {
      dtoOperations.createAllProjections(suffix, shellContext);
    }
  }

  /**
   * Replaces a JavaType fullyQualifiedName for a shorter name using '~' for
   * TopLevelPackage
   *
   * @param cid ClassOrInterfaceTypeDetails of a JavaType
   * @param currentText String current text for option value
   * @return the String representing a JavaType with its name shortened
   */
  private String replaceTopLevelPackageString(ClassOrInterfaceTypeDetails cid, String currentText) {
    String javaTypeFullyQualilfiedName = cid.getType().getFullyQualifiedTypeName();
    String javaTypeString = "";
    String topLevelPackageString = "";

    // Add module value to topLevelPackage when necessary
    if (StringUtils.isNotBlank(cid.getType().getModule())
        && !cid.getType().getModule().equals(projectOperations.getFocusedModuleName())) {

      // Target module is not focused
      javaTypeString = cid.getType().getModule().concat(LogicalPath.MODULE_PATH_SEPARATOR);
      topLevelPackageString =
          projectOperations.getTopLevelPackage(cid.getType().getModule())
              .getFullyQualifiedPackageName();
    } else if (StringUtils.isNotBlank(cid.getType().getModule())
        && cid.getType().getModule().equals(projectOperations.getFocusedModuleName())
        && (currentText.startsWith(cid.getType().getModule()) || cid.getType().getModule()
            .startsWith(currentText)) && StringUtils.isNotBlank(currentText)) {

      // Target module is focused but user wrote it
      javaTypeString = cid.getType().getModule().concat(LogicalPath.MODULE_PATH_SEPARATOR);
      topLevelPackageString =
          projectOperations.getTopLevelPackage(cid.getType().getModule())
              .getFullyQualifiedPackageName();
    } else {

      // Not multimodule project
      topLevelPackageString =
          projectOperations.getFocusedTopLevelPackage().getFullyQualifiedPackageName();
    }

    // Autocomplete with abbreviate or full qualified mode
    String auxString =
        javaTypeString.concat(StringUtils.replace(javaTypeFullyQualilfiedName,
            topLevelPackageString, "~"));
    if ((StringUtils.isBlank(currentText) || auxString.startsWith(currentText))
        && StringUtils.contains(javaTypeFullyQualilfiedName, topLevelPackageString)) {

      // Value is for autocomplete only or user wrote abbreviate value
      javaTypeString = auxString;
    } else {

      // Value could be for autocomplete or for validation
      javaTypeString = String.format("%s%s", javaTypeString, javaTypeFullyQualilfiedName);
    }

    return javaTypeString;
  }

  /**
   * Gets a list of fields from an entity. Static and transient fields are excluded
   * as well as collection fields which type is an entity.
   *
   * @param entity the JavaType from which to obtain the field list.
   * @return a List<FieldMetadata> with info of the entity fields.
   */
  private List<FieldMetadata> getEntityFieldList(JavaType entity) {
    List<FieldMetadata> fieldList = new ArrayList<FieldMetadata>();
    ClassOrInterfaceTypeDetails typeDetails = typeLocationService.getTypeDetails(entity);
    Validate.notNull(typeDetails, String.format(
        "Cannot find details for class %s. Please be sure that the class exists.",
        entity.getFullyQualifiedTypeName()));
    MemberDetails entityMemberDetails =
        memberDetailsScanner.getMemberDetails(this.getClass().getName(), typeDetails);
    Validate.notNull(
        entityMemberDetails.getAnnotation(JpaJavaType.ENTITY),
        String.format("%s must be an entity to obtain it's fields info.",
            entity.getFullyQualifiedTypeName()));

    // Get fields and check for other fields from relations
    List<FieldMetadata> entityFields = entityMemberDetails.getFields();
    for (FieldMetadata field : entityFields) {

      // Exclude static fields
      if (Modifier.isStatic(field.getModifier())) {
        continue;
      }

      // Exclude transient fields
      if (field.getAnnotation(JpaJavaType.TRANSIENT) != null) {
        continue;
      }

      // Exclude entity collection fields
      JavaType fieldType = field.getFieldType();
      if (fieldType.isCommonCollectionType()) {
        boolean isEntityCollectionField = false;
        List<JavaType> parameters = fieldType.getParameters();
        for (JavaType parameter : parameters) {
          if (typeIsEntity(parameter)) {
            isEntityCollectionField = true;
            break;
          }
        }

        if (isEntityCollectionField) {
          continue;
        }
      }

      fieldList.add(field);
    }

    return fieldList;
  }

  /**
   * Tries to obtain JavaType indicated in command or which has the focus in the
   * Shell
   *
   * @param shellContext the Roo Shell context
   * @return JavaType or null if no class has the focus or no class is specified
   *         in the command
   */
  private JavaType getTypeFromEntityParam(ShellContext shellContext) {
    // Try to get 'class' from ShellContext
    String typeString = shellContext.getParameters().get("entity");
    JavaType type = null;
    if (typeString != null) {
      type = getJavaTypeConverter().convertFromText(typeString, JavaType.class, PROJECT);
    } else {
      type = lastUsed.getJavaType();
    }

    // Inform that entity param couldn't be retrieved
    Validate.notNull(type,
        "Couldn't get the entity for 'entity projection' command. Please, be sure that "
            + "param '--entity' is specified with a right value.");

    return type;
  }

  private boolean typeIsEntity(JavaType type) {
    return typeLocationService.getTypeDetails(type) != null
        && typeLocationService.getTypeDetails(type).getAnnotation(RooJavaType.ROO_JPA_ENTITY) != null;
  }

  @SuppressWarnings("unchecked")
  public Converter<JavaType> getJavaTypeConverter() {
    if (javaTypeConverter == null) {

      // Get all Services implement JavaTypeConverter interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(Converter.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          Converter<?> converter = (Converter<?>) this.context.getService(ref);
          if (converter.supports(JavaType.class, PROJECT)) {
            javaTypeConverter = (Converter<JavaType>) converter;
            return javaTypeConverter;
          }
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("ERROR: Cannot load JavaTypeConverter on FieldCommands.");
        return null;
      }
    } else {
      return javaTypeConverter;
    }
  }

}
