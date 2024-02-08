package uk.co.odinconsultants.sql

object SqlServerUtils {

  def ddlIfTableDoesNotExist(tableName: String, conditional: String): String =
    s"""
       |    if not exists (select * from sysobjects where name='${tableName}' and xtype='U')
       |        $conditional
       |            """.stripMargin

}
