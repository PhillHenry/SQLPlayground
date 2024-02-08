package uk.co.odinconsultants.sql

import java.lang.reflect.Field

object SqlUtils {

  def toSimpleName(x: Class[?]): String = x.getSimpleName

  def ddlFields(clazz: Class[?]): String = {
    val jvmToSql: Map[Class[?], String] = Map(classOf[String] -> "VARCHAR(1024)").withDefault { toSimpleName }
    clazz.getDeclaredFields.toList
      .map { case(field: Field) =>
        s"${field.getName} ${jvmToSql(field.getType)}"
      }
      .mkString(",\n ")
  }

}
