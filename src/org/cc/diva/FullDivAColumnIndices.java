package org.cc.diva;

/**
 * Created by crco0001 on 5/11/2016.
 */
public enum FullDivAColumnIndices {


    PID(0),
    Name(1),
    Title(2),
    PublicationType(3),
    ContentType(4),
    Language(5),
    Journal(6),
    JournalISSN(7),
    JournalEISSN(8), // new
    Status(9),
    Volume(10),
    Issue(11),
    HostPublication(12),
    StartPage(13),
    EndPage(14),
    Year(15),
    Edition(16),
    Pages(17),
    City(18),
    Publisher(19),
    Series(20),
    SeriesEISSN(21), // new
    SeriesISSN(22),
    ISBN(23),
    Urls(24),
    ISRN(25),
    DOI(26),
    ISI(27),
    PMID(28),
    ScopusId(29),
    NBN(30),
    LocalId(31),
    ArchiveNumber(32),
    Keywords(33),
    Categories(34),
    ResearchSubjects(35),
    Projects(36),
    Notes(37),
    Abstract(38),
    Opponents(39),
    Supervisors(40),
    Examiners(41),
    Patent(42),
    ThesisLevel(43),
    Credits(44),
    Programme(45),
    Subject(46),
    Uppsok(47),
    DefencePlace(48),
    DefenceLanguage(49),
    DefenceDate(50),
    CreatedDate(51),
    PublicationDate(52),
    LastUpdated(53),
    FullTextLink(54),
    Funder(55),
    NumberOfAuthors(56),
    ExternalCooperation(57),
    FridaLevel(58),
    PartOfThesis(59),
    PublicationSubtype(60),
    Conference(61),
    Term(62),
    ArticleId(63),
    Reviewed(64),
    FreeFulltext(65),
    SustainableDevelopment(66),
    Contributor(67);

    private int index;

    FullDivAColumnIndices(int ind) {
        index = ind;
    }

    public int getValue() {
        return index;
    }
}
