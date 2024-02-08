package uk.co.odinconsultants.sql

import java.lang.reflect.Field

object SqlUtils {

  def ddlFields(clazz: Class[?]): String =
    clazz.getDeclaredFields.toList
      .map { case(field: Field) =>
        s"${field.getName} ${field.getType.getSimpleName}"
      }
      .mkString(",\n ")

}
