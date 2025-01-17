package com.agilogy.timetracking.driveradapters.console

import arrow.core.raise.Raise
import com.agilogy.timetracking.domain.DeveloperName
import com.agilogy.timetracking.domain.ProjectName
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

data class ArgsParseError(val message: String)

object ArgsParser {

    context(Raise<ArgsParseError>)
    fun parse(args: List<String>): Command {
        fun arg(index: Int): String = if (index < args.size) args[index] else ""

        return if (args.isEmpty()) Help
        else if (arg(0) == "report") when (args.size - 1) {
            0 -> GlobalReport(YearMonth.now())
            1 -> GlobalReport(parseMonth(arg(1)))
            2 -> DeveloperReport(parseMonth(arg(1)), DeveloperName(arg(2)))
            else -> raise(ArgsParseError("Invalid number of arguments for command ${arg(0)}"))
        } else if (arg(0) == "list") {
            ListTimeEntries(parseMonth(arg(1)), args.getOrElse(2) { null }?.let { DeveloperName(it) })
        } else if (arg(0) == "add") {
            val zoneId = parseZoneId(arg(5))
            AddTimeEntry(
                DeveloperName(arg(1)),
                ProjectName(arg(2)),
                parseInstant(arg(3), zoneId)..parseInstant(arg(4), zoneId),
                zoneId,
            )
        } else raise(ArgsParseError("Unknown command ${arg(0)}"))
    }

    context(Raise<ArgsParseError>)
    private fun parseMonth(value: String): YearMonth = parse("month", value) { YearMonth.parse(it) }

    context(Raise<ArgsParseError>)
    private fun parseInstant(value: String, zoneId: ZoneId): Instant = parse("instant", value) {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ENGLISH).withZone(zoneId)
        ZonedDateTime.parse(value, formatter).toInstant()
    }

    context(Raise<ArgsParseError>)
    private fun parseZoneId(value: String): ZoneId = parse("zoneid", value) { ZoneId.of(it) }

    context(Raise<ArgsParseError>)
    private fun <A> parse(type: String, value: String, parse: (String) -> A): A =
        runCatching { parse(value) }.getOrElse { raise(ArgsParseError("Invalid $type $value")) }

    fun help(): String =
        """
    Usage: timetracking <command> [options]
    
    Commands:
      add   <developer> <project> <start> <end>  Adds a new time entry
          developer: developer name
          project: project name
          start: start time in the format yyyy-MM-dd HH:mm
          end: end time in the format yyyy-MM-dd HH:mm or just HH:mm
          zoneId: Timezone identifier like Europe/Monaco
          
          Example:
            add pepe teto "2023-01-10 08:00" "2023-01-10 17:00" Europe/Madrid
          
      list   <month> [<developer?]               List the time entries for the given month and developer
          month: month in the format yyyy-MM
          developer: developer name
      report [<month>]                           Show the global time tracking report for the given month
          month: month in the format yyyy-MM, defaults to current month
      report <month> <developer>                 Show the time tracking report for the given developer and month
          month: month in the format yyyy-MM
          developer: developer name
          
        """.trimIndent()
}
