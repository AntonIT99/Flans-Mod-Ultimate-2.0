package com.flansmodultimate.common.eras;

import com.flansmodultimate.common.types.EnumType;
import com.flansmodultimate.common.types.TypeFile;
import com.flansmodultimate.config.Category;
import com.flansmodultimate.content.ContentPack;
import com.flansmodultimate.content.IContentProvider;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ServicePeriodReaderTest
{
    private static final EraCatalog ERAS = EraCatalog.defaults();
    private static final IContentProvider PACK = new ContentPack("test", Path.of("build", "test-packs", "eras"));

    private static TypeFile file(String... lines)
    {
        return new TypeFile("synthetic", EnumType.GUN, PACK, List.of(lines));
    }

    private static ServicePeriodReader.Result read(TypeFile file)
    {
        return ServicePeriodReader.read(file, ERAS);
    }

    @Test
    void undatedTypeHasNoPeriod()
    {
        ServicePeriodReader.Result result = read(file("ShortName m4a1"));
        assertNull(result.period());
        assertTrue(result.warnings().isEmpty());
    }

    @Test
    void readsIntroductionAndRetirement()
    {
        ServicePeriodReader.Result result = read(file("IssueDate 1942-02", "RetirementDate Cold War"));
        assertEquals("1942-02 coldwar", String.valueOf(result.period()));
        assertTrue(result.warnings().isEmpty());
    }

    @Test
    void retirementAloneIsIgnored()
    {
        ServicePeriodReader.Result result = read(file("RetirementDate 1957"));
        assertNull(result.period());
        assertEquals(1, result.warnings().size());
    }

    @Test
    void malformedValuesAreReported()
    {
        ServicePeriodReader.Result badIssue = read(file("IssueDate 1944/06"));
        assertNull(badIssue.period());
        assertEquals(1, badIssue.warnings().size());
        ServicePeriodReader.Result badRetirement = read(file("IssueDate 1944", "RetirementDate 1944-13"));
        assertEquals("1944", String.valueOf(badRetirement.period()));
        assertEquals(1, badRetirement.warnings().size());
    }

    @Test
    void reversedRetirementIsDropped()
    {
        ServicePeriodReader.Result result = read(file("IssueDate 1944", "RetirementDate 1940"));
        assertEquals("1944", String.valueOf(result.period()));
        assertEquals(1, result.warnings().size());
    }

    @Test
    void unknownEraIsKeptWithAWarning()
    {
        ServicePeriodReader.Result result = read(file("IssueDate Atlantis"));
        assertNotNull(result.period());
        assertEquals(1, result.warnings().size());
    }

    @Test
    void categoryDateReplacesThePackDateByDefault()
    {
        TypeFile file = file("ShortName m4a1", "IssueDate 1943");
        file.addCategoryConfigMap(category("1942-02", Map.of()), "m4a1");
        assertEquals("1942-02", String.valueOf(read(file).period()));
    }

    @Test
    void ifAbsentCategoryDateDefersToThePack()
    {
        TypeFile dated = file("ShortName m4a1", "IssueDate 1943");
        dated.addCategoryConfigMap(category("1942-02", Map.of(ServicePeriodReader.ISSUE_DATE, "ifAbsent")), "m4a1");
        assertEquals("1943", String.valueOf(read(dated).period()));

        TypeFile undated = file("ShortName m4a1");
        undated.addCategoryConfigMap(category("1942-02", Map.of(ServicePeriodReader.ISSUE_DATE, "ifAbsent")), "m4a1");
        assertEquals("1942-02", String.valueOf(read(undated).period()));
    }

    private static Category category(String issueDate, Map<String, String> modes)
    {
        Category category = new Category(EnumType.GUN, "M4 carbine");
        category.getProperties().put(ServicePeriodReader.ISSUE_DATE, List.of(issueDate));
        category.getPropertyModes().putAll(modes);
        category.getItems().add("m4a1");
        return category;
    }
}
