package com.flansmodultimate.common.eras;

import com.flansmodultimate.common.types.TypeFile;
import com.flansmodultimate.util.TypeReaderUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads the optional {@code IssueDate} and {@code RetirementDate} keys every type accepts. Categories set them through
 * their {@code properties} like any other key, so the last value given wins and {@code propertyModes} decide whether a
 * category replaces a pack's date or only fills a missing one.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ServicePeriodReader
{
    public static final String ISSUE_DATE = "IssueDate";
    public static final String RETIREMENT_DATE = "RetirementDate";

    public record Result(@Nullable ServicePeriod period, List<String> warnings)
    {}

    /** The period the file gives, or none, with a warning for each value that had to be ignored. */
    public static Result read(TypeFile file, EraCatalog eras)
    {
        List<String> warnings = new ArrayList<>();
        String issueText = TypeReaderUtils.readValues(ISSUE_DATE, null, file);
        String retirementText = TypeReaderUtils.readValues(RETIREMENT_DATE, null, file);
        if (issueText == null)
        {
            if (retirementText != null)
                warnings.add(RETIREMENT_DATE + " is ignored without an " + ISSUE_DATE);
            return new Result(null, warnings);
        }
        ServiceDate issue = parse(ISSUE_DATE, issueText, eras, warnings);
        if (issue == null)
            return new Result(null, warnings);
        ServiceDate retirement = retirementText == null ? null : parse(RETIREMENT_DATE, retirementText, eras, warnings);
        ServicePeriod period = new ServicePeriod(issue, retirement);
        if (!period.isOrdered(eras))
        {
            warnings.add(RETIREMENT_DATE + " '" + retirement + "' ends before " + ISSUE_DATE + " '" + issue + "' starts; ignoring the retirement");
            period = new ServicePeriod(issue, null);
        }
        return new Result(period, warnings);
    }

    @Nullable
    private static ServiceDate parse(String key, String text, EraCatalog eras, List<String> warnings)
    {
        try
        {
            ServiceDate date = ServiceDate.parse(text);
            // Kept anyway: the era may be added to eras.json later, and until then the item counts as undated.
            if (!date.isResolvable(eras))
                warnings.add(key + " names unknown era '" + text.strip() + "'; the item counts as undated until eras.json defines it");
            return date;
        }
        catch (IllegalArgumentException exception)
        {
            warnings.add(key + ": " + exception.getMessage());
            return null;
        }
    }
}
