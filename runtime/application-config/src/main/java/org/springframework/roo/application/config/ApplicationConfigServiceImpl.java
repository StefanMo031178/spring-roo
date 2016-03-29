package org.springframework.roo.application.config;

import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.propfiles.manager.PropFilesManagerService;
import org.springframework.roo.settings.project.ProjectSettingsService;

/**
 * Provides a service to manage all necessary properties located on application
 * configuration files.
 * 
 * @author Juan Carlos García
 * @author Paula Navarro
 * @since 2.0
 */
@Component
@Service
public class ApplicationConfigServiceImpl implements ApplicationConfigService {

  private static final Path DEFAULT_APPLICATION_CONFIG_FILE_LOCATION = Path.SRC_MAIN_RESOURCES;
  private static final String DEFAULT_APPLICATION_CONFIG_FILE_NAME = "application";
  private static final String DEFAULT_APPLICATION_CONFIG_FILE_EXTENSION = ".properties";

  // ROO-3706: Spring Roo Configuration Settings
  private static final String CONFIG_FILE_LOCATION_SHELL_PROPERTY =
      "spring.roo.configuration.location";
  private static final String CONFIG_FILE_NAME_SHELL_PROPERTY = "spring.roo.configuration.name";

  @Reference
  private PropFilesManagerService propFilesManager;
  @Reference
  private PathResolver pathResolver;
  @Reference
  private FileManager fileManager;
  @Reference
  private ProjectSettingsService settingsService;
  @Reference
  private TypeLocationService typeLocationService;

  @Override
  public void addProperty(final String key, final String value, String profile, boolean force) {

    // Get application modules
    for (String moduleName : typeLocationService.getApplicationModules()) {
      propFilesManager.addPropertyIfNotExists(getApplicationConfigFileLocation(moduleName),
          getAppliCationConfigFileName(profile), key, value, true, force);
    }
  }

  @Override
  public void addProperty(final String prefix, final String key, final String value,
      String profile, boolean force) {
    // Get application modules
    for (String moduleName : typeLocationService.getApplicationModules()) {
      propFilesManager.addPropertyIfNotExists(getApplicationConfigFileLocation(moduleName),
          getAppliCationConfigFileName(profile), prefix, key, value, true, force);
    }
  }

  @Override
  public void addProperties(final Map<String, String> properties, String profile, boolean force) {
    // Get application modules
    for (String moduleName : typeLocationService.getApplicationModules()) {
      propFilesManager.addProperties(getApplicationConfigFileLocation(moduleName),
          getAppliCationConfigFileName(profile), properties, true, force);
    }
  }

  @Override
  public void addProperties(final String prefix, final Map<String, String> properties,
      String profile, boolean force) {
    // Get application modules
    for (String moduleName : typeLocationService.getApplicationModules()) {
      propFilesManager.addProperties(getApplicationConfigFileLocation(moduleName),
          getAppliCationConfigFileName(profile), prefix, properties, true, force);
    }
  }

  @Override
  public void updateProperty(final String key, final String value, String profile, boolean force) {
    for (String moduleName : typeLocationService.getApplicationModules()) {
      propFilesManager.changeProperty(getApplicationConfigFileLocation(moduleName),
          getAppliCationConfigFileName(profile), key, value, true, force);
    }
  }

  @Override
  public void updateProperty(final String prefix, final String key, final String value,
      String profile, boolean force) {
    for (String moduleName : typeLocationService.getApplicationModules()) {
      propFilesManager.changeProperty(getApplicationConfigFileLocation(moduleName),
          getAppliCationConfigFileName(profile), prefix, key, value, true, force);
    }
  }

  @Override
  public void updateProperties(final Map<String, String> properties, String profile, boolean force) {
    for (String moduleName : typeLocationService.getApplicationModules()) {
      propFilesManager.changeProperties(getApplicationConfigFileLocation(moduleName),
          getAppliCationConfigFileName(profile), properties, true, force);
    }
  }

  @Override
  public void updateProperties(final String prefix, final Map<String, String> properties,
      String profile, boolean force) {
    for (String moduleName : typeLocationService.getApplicationModules()) {
      propFilesManager.changeProperties(getApplicationConfigFileLocation(moduleName),
          getAppliCationConfigFileName(profile), prefix, properties, true, force);
    }
  }

  @Override
  public Map<String, String> getProperties(String profile, String moduleName) {
    return propFilesManager.getProperties(getApplicationConfigFileLocation(moduleName),
        getAppliCationConfigFileName(profile));
  }

  @Override
  public SortedSet<String> getPropertyKeys(boolean includeValues, String profile) {
    SortedSet<String> keys = new TreeSet<String>();
    for (String moduleName : typeLocationService.getApplicationModules()) {
      keys.addAll(propFilesManager.getPropertyKeys(getApplicationConfigFileLocation(moduleName),
          getAppliCationConfigFileName(profile), includeValues));
    }
    return keys;
  }

  @Override
  public SortedSet<String> getPropertyKeys(String prefix, boolean includeValues, String profile) {
    SortedSet<String> keys = new TreeSet<String>();
    for (String moduleName : typeLocationService.getApplicationModules()) {
      keys.addAll(propFilesManager.getPropertyKeys(getApplicationConfigFileLocation(moduleName),
          getAppliCationConfigFileName(profile), prefix, includeValues));
    }
    return keys;
  }

  @Override
  public String getProperty(final String key, String profile) {
    for (String moduleName : typeLocationService.getApplicationModules()) {
      String property =
          propFilesManager.getProperty(getApplicationConfigFileLocation(moduleName),
              getAppliCationConfigFileName(profile), key);
      if (property != null) {
        return property;
      }
    }
    return null;
  }

  @Override
  public String getProperty(final String prefix, final String key, String profile) {
    for (String moduleName : typeLocationService.getApplicationModules()) {
      String property =
          propFilesManager.getProperty(getApplicationConfigFileLocation(moduleName),
              getAppliCationConfigFileName(profile), prefix, key);
      if (property != null) {
        return property;
      }
    }
    return null;
  }

  @Override
  public void removeProperty(final String key, String profile) {
    for (String moduleName : typeLocationService.getApplicationModules()) {
      if (existsSpringConfigFile(profile, moduleName)) {
        propFilesManager.removeProperty(getApplicationConfigFileLocation(moduleName),
            getAppliCationConfigFileName(profile), key);
      }
    }
  }

  @Override
  public void removeProperty(final String prefix, String key, String profile) {
    for (String moduleName : typeLocationService.getApplicationModules()) {
      if (existsSpringConfigFile(profile, profile)) {
        propFilesManager.removeProperty(getApplicationConfigFileLocation(moduleName),
            getAppliCationConfigFileName(profile), prefix, key);
      }
    }
  }

  @Override
  public void removeProperties(List<String> keys, String profile) {
    for (String moduleName : typeLocationService.getApplicationModules()) {
      for (String key : keys) {
        removeProperty(key, profile, moduleName);
      }
    }
  }

  @Override
  public void removePropertiesByPrefix(String prefix, String profile) {
    for (String moduleName : typeLocationService.getApplicationModules()) {
      if (existsSpringConfigFile(profile, moduleName)) {
        propFilesManager.removePropertiesByPrefix(getApplicationConfigFileLocation(moduleName),
            getAppliCationConfigFileName(profile), prefix);
      }
    }
  }

  @Override
  public String getSpringConfigLocation(String moduleName) {
    return pathResolver.getIdentifier(getApplicationConfigFileLocation(moduleName),
        getAppliCationConfigFileName(null));
  }

  @Override
  public String getSpringConfigLocation(String profile, String moduleName) {
    return pathResolver.getIdentifier(getApplicationConfigFileLocation(moduleName),
        getAppliCationConfigFileName(profile));
  }

  @Override
  public boolean existsSpringConfigFile(String moduleName) {
    return fileManager.exists(getSpringConfigLocation(moduleName));
  }

  @Override
  public boolean existsSpringConfigFile(String profile, String moduleName) {
    return fileManager.exists(getSpringConfigLocation(profile, moduleName));
  }

  /**
   * Method that generates application config file name using application
   * config file name profile parameter if exists, and config file extension.
   * 
   * @param profile
   * @return
   */
  private String getAppliCationConfigFileName(String profile) {
    String fileName = DEFAULT_APPLICATION_CONFIG_FILE_NAME;

    // ROO-3706: Check if exists some specific configuration to the
    // name of application config properties file
    String fileNameShellConfig = settingsService.getProperty(CONFIG_FILE_NAME_SHELL_PROPERTY);
    if (fileNameShellConfig != null && StringUtils.isNotBlank(fileNameShellConfig)) {
      fileName = fileNameShellConfig;
    }

    if (profile != null && StringUtils.isNotBlank(profile)) {
      fileName = fileName.concat("-").concat(profile);
    }
    fileName = fileName.concat(DEFAULT_APPLICATION_CONFIG_FILE_EXTENSION);

    return fileName;
  }

  /**
   * Method that get application config file location
   * 
   * @return Path where application config file is located
   */
  private LogicalPath getApplicationConfigFileLocation(String moduleName) {

    LogicalPath location =
        LogicalPath.getInstance(DEFAULT_APPLICATION_CONFIG_FILE_LOCATION, moduleName);

    // ROO-3706: Check if exists some specific configuration to the
    // location of application config properties file
    String fileLocationShellConfig =
        settingsService.getProperty(CONFIG_FILE_LOCATION_SHELL_PROPERTY);
    if (fileLocationShellConfig != null && StringUtils.isNotBlank(fileLocationShellConfig)) {
      location = LogicalPath.getInstance(Path.ROOT, fileLocationShellConfig);
    }

    return location;
  }

}
