package org.cc.diva;

/**
 * Created by crco0001 on 5/11/2016.
 */
public enum ReducedDiVAColumnIndices {

    PID(0),
    Name(1),
    Title(2),
    PublicationType(3),
    ContentType(4),
    Language(5),
    Journal(6),
    JournalISSN(7),
    Status(8),
    Volume(9),
    Issue(10),
    HostPublication(11),
    Year(12),
    Edition(13),
    Pages(14),
    Publisher(15),
    Series(16),
    SeriesISSN(17),
    ISBN(18),
    DOI(19),
    ISI(20),
    PMID(21),
    ScopusId(22),
    NBN(23),
    Keywords(24),
    Categories(25),
    Notes(26),
    Abstract(27),
    CreatedDate(28),
    PublicationDate(29),
    LastUpdated(30),
    NumberOfAuthors(31),
    PartOfThesis(32),
    PublicationSubtype(33),
    ArticleId(34),
    Reviewed(35),
    FreeFulltext(36),
    ContributorString(37),
    LocalId(38);

    private int index;

    ReducedDiVAColumnIndices(int ind) {
        index = ind;
    }

    public int getValue() {
        return index;
    }

}
