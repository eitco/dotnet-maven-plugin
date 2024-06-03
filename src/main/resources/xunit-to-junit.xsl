<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:trx="http://microsoft.com/schemas/VisualStudio/TeamTest/2010"
>


    <xsl:template match="//trx:TestRun">

        <testsuite errors="0">
            <xsl:attribute name="name">
                <xsl:value-of select="@name"/>
            </xsl:attribute>
            <xsl:attribute name="tests">
                <xsl:value-of select="count(trx:Results/trx:UnitTestResult)"/>
            </xsl:attribute>
            <xsl:attribute name="failures">
                <xsl:value-of select="count(trx:Results/trx:UnitTestResult[@outcome = 'Failed'])"/>
            </xsl:attribute>
            <xsl:attribute name="skipped">
                <xsl:value-of select="count(trx:Results/trx:UnitTestResult[@outcome = 'NotExecuted'])"/>
            </xsl:attribute>
            <xsl:attribute name="timestamp">
                <xsl:value-of select="trx:Times/@creation"/>
            </xsl:attribute>
            <xsl:apply-templates select="trx:Results/trx:UnitTestResult"/>
        </testsuite>

    </xsl:template>

    <xsl:template match="trx:UnitTestResult">
        <testcase>
            <xsl:attribute name="className">
                <xsl:value-of select="@testId"/>
            </xsl:attribute>
            <xsl:attribute name="name">
                <xsl:value-of select="@testName"/>
            </xsl:attribute>
            <xsl:attribute name="time">
                <xsl:value-of select="@duration"/>
            </xsl:attribute>
            <xsl:if test="@outcome = 'Failed'">
                <failure>
                    <xsl:attribute name="message">
                        <xsl:value-of select="trx:OutPut/trx:ErrorInfo/trx:Message"/>
                    </xsl:attribute>
                    <xsl:value-of select="trx:OutPut/trx:ErrorInfo/trx:StackTrace"/>
                </failure>
            </xsl:if>
            <xsl:if test="@outcome = 'NotExecuted'">
                <skipped/>
            </xsl:if>
        </testcase>
    </xsl:template>

</xsl:stylesheet>