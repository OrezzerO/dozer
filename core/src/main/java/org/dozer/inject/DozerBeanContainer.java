package org.dozer.inject;

import org.dozer.MappingException;
import org.dozer.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Dmitry Buzdin
 */
public class DozerBeanContainer implements BeanRegistry {

  private Map<Class<?>, Object[]> beans = new ConcurrentHashMap<Class<?>, Object[]>();

  public void register(Class<?> type) {
    beans.put(type, new Object[1]);
  }

  public <T> T getBean(Class<T> type) {
    Collection<T> result = getBeans(type);
    if (result.isEmpty()) {
      throw new IllegalStateException("Bean is not registered : " + type.getName());
    }
    if (result.size() > 1) {
      throw new IllegalStateException("More than one bean found of type : " + type.getName());
    }

    return result.iterator().next();
  }

  public <T> Collection<T> getBeans(Class<T> type) {
    HashSet<T> result = new HashSet<T>();
    for (Map.Entry<Class<?>, Object[]> entry : beans.entrySet()) {
      if (type.isAssignableFrom(entry.getKey())) {
        Object[] value = entry.getValue();
        if (value[0] == null) {
          value[0] = wireBean(entry.getKey());
        }
        result.add((T) value[0]);
      }
    }
    return result;
  }

  private <T> T wireBean(Class<T> type) {
    final T bean = ReflectionUtils.newInstance(type);

    Field[] destFields = type.getDeclaredFields();
    for (Field field : destFields) {
      Inject inject = field.getAnnotation(Inject.class);
      if (inject != null) {
        Class<?> fieldType = field.getType();
        try {
          if (field.get(bean) == null) {
            Object dependency = wireBean(fieldType);
            field.set(bean, dependency);
          }
        } catch (IllegalAccessException e) {
          throw new MappingException("Field annotated with @Inject is not accessible : " + field.getName(), e);
        }
      }
    }

    Object[] instance = beans.get(type);
    if (instance == null) {
      instance = new Object[1];
      beans.put(type, instance);
    }
    instance[0] = bean;
    return bean;
  }

}