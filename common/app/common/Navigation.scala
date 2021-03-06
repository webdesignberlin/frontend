package common

import model.{Content, MetaData}
import play.api.mvc.RequestHeader
import conf.Switches._
import dev.HttpSwitch

case class SectionLink(zone: String, title: String, breadcrumbTitle: String, href: String) {
  def currentFor(page: MetaData): Boolean = page.url == href ||
    s"/${page.section}" == href ||
    (Edition.all.exists(_.id.toLowerCase == page.id.toLowerCase) && href == "/")

  def currentForIncludingAllTags(page: MetaData): Boolean = page.tags.exists(t => s"/${t.id}" == href)
}

case class NavItem(name: SectionLink, links: Seq[SectionLink] = Nil) {
  def currentFor(page: MetaData): Boolean = name.currentFor(page) ||
    links.exists(_.currentFor(page)) || exactFor(page)

  def currentForIncludingAllTags(page: MetaData): Boolean = name.currentForIncludingAllTags(page) ||
    links.exists(_.currentForIncludingAllTags(page))

  def searchForCurrentSublink(page: MetaData)(implicit request: RequestHeader): Option[SectionLink] = {
    val localHrefs = links.map(_.href)
    val currentHref = page.tags.find(tag => localHrefs.contains(tag.url)).map(_.url).getOrElse("")
    links.find(_.href == currentHref)
      .orElse(links.find(_.currentFor(page)))
      .orElse(links.find(_.currentForIncludingAllTags(page)))
  }

  def exactFor(page: MetaData): Boolean = page.section == name.href.dropWhile(_ == '/') || page.url == name.href
}

trait Navigation {

  //News
  val home = SectionLink("news", "home", "Home", "/")
  val news = SectionLink("news", "news", "News", "/")
  val world = SectionLink("world", "world", "World", "/world")
  val uk = SectionLink("uk-news", "UK", "UK News", "/uk-news")
  val us = SectionLink("world", "US", "US News", "/world/usa")
  val politics = SectionLink("politics", "politics", "Politics", "/politics")
  val technology = SectionLink("technology", "tech", "Technology", "/technology")
  val environment = SectionLink("environment", "environment", "Environment", "/environment")
  val media = SectionLink("media", "media", "Media", "/media")
  val education = SectionLink("education", "education", "Education", "/education")
  val students = SectionLink("education", "students", "Students", "/education/students")
  val society = SectionLink("society", "society", "Society", "/society")
  val development = SectionLink("globaldevelopment", "global development", "Global development", "/global-development")
  val science = SectionLink("science", "science", "Science", "/science")
  val law = SectionLink("law", "law", "Law", "/law")
  val blogs = SectionLink("blogs", "blogs", "Blogs", "/tone/blog")
  val inpictures = SectionLink("inpictures", "galleries", "In pictures", "/inpictures")
  val europeNews = SectionLink("world", "europe", "Europe", "/world/europe-news")
  val americas = SectionLink("world", "americas", "Americas", "/world/americas")
  val australia = SectionLink("world", "australia", "Australia", "/world/australia")
  val asia = SectionLink("world", "asia", "Asia", "/world/asia")
  val africa = SectionLink("world", "africa", "Africa", "/world/africa")
  val middleEast = SectionLink("world", "middle east", "Middle east", "/world/middleeast")
  val video = SectionLink("video", "video", "Video", "/video")
  val observer = SectionLink("observer", "observer", "Observer", "/observer")

  val health = SectionLink("society", "health", "Health", "/society/health")

  //Sport
  val sport = SectionLink("sport", "sport", "Sport", "/sport")
  val sports = sport.copy(title = "sports", breadcrumbTitle = "Sports")
  val usSport = SectionLink("sport", "US sports", "US sports", "/sport/us-sport")
  val australiaSport = SectionLink("australia sport", "australia sport", "Australia sport", "/sport/australia-sport")
  val afl = SectionLink("afl", "afl", "afl", "/sport/afl")
  val nrl = SectionLink("nrl", "nrl", "nfl", "/sport/nrl")
  val aLeague = SectionLink("a-league", "a-league", "A-league", "/football/a-league")
  val football = SectionLink("football", "football", "Football", "/football")
  val soccer = football.copy(title = "soccer", breadcrumbTitle = "Soccer")
  val cricket = SectionLink("sport", "cricket", "Cricket", "/sport/cricket")
  val sportblog = SectionLink("sport", "sport blog", "Sport blog", "/sport/blog")
  val cycling = SectionLink("sport", "cycling", "Cycling", "/sport/cycling")
  val rugbyunion = SectionLink("sport", "rugby union", "Rugby union", "/sport/rugby-union")
  val rugbyLeague = SectionLink("sport", "rugby league", "Rugby union", "/sport/rugbyleague")
  val motorsport = SectionLink("sport", "motor sport", "Motor sport", "/sport/motorsports")
  val tennis = SectionLink("sport", "tennis", "Tennis", "/sport/tennis")
  val golf = SectionLink("sport", "golf", "Golf", "/sport/golf")
  val horseracing = SectionLink("sport", "horse racing", "Horse racing", "/sport/horse-racing")
  val boxing = SectionLink("sport", "boxing", "Boxing", "/sport/boxing")
  val formulaOne = SectionLink("sport", "F1", "Formula one", "/sport/formulaone")
  val racing = SectionLink("sport", "racing", "Racing", "/sport/horse-racing")

  val nfl = SectionLink("sport", "NFL", "NFL", "/sport/nfl")
  val mlb = SectionLink("sport", "MLB", "MLB", "/sport/mlb")
  val nba = SectionLink("sport", "NBA", "NBA", "/sport/nba")
  val mls = SectionLink("football", "MLS", "MLS", "/football/mls")
  val nhl = SectionLink("sport", "NHL", "NHL", "/sport/nhl")

  //Cif
  val cif = SectionLink("commentisfree", "comment", "Comment", "/commentisfree")
  val opinion = SectionLink("commentisfree", "opinion", "Opinion", "/commentisfree")
  val cifbelief = SectionLink("commentisfree", "cif belief", "Cif belief", "/commentisfree/belief")
  val cifgreen = SectionLink("commentisfree", "cif green", "Cif green", "/commentisfree/cif-green")

  //Culture
  val culture = SectionLink("culture", "culture", "Culture", "/culture")
  val artanddesign = SectionLink("culture", "art & design", "Art & design", "/artanddesign")
  val books = SectionLink("culture", "books", "Books", "/books")
  val film = SectionLink("culture", "film", "Film", "/film")
  val movies = film.copy(title = "movies", breadcrumbTitle = "Movies")
  val music = SectionLink("culture", "music", "Music", "/music")
  val stage = SectionLink("culture", "stage", "Stage", "/stage")
  val televisionAndRadio = SectionLink("culture", "tv & radio", "TV & radio", "/tv-and-radio")
  val classicalMusic = SectionLink("classical", "classical", "Classical", "/music/classicalmusicandopera")

  //Technology
  val technologyblog = SectionLink("technology", "technology blog", "Technology blog", "/technology/blog")
  val games = SectionLink("technology", "games", "Games", "/technology/games")
  val gamesblog = SectionLink("technology", "games blog", "Games blog", "/technology/gamesblog")
  val appsblog = SectionLink("technology", "apps blog", "Apps blog", "/technology/appsblog")
  val askjack = SectionLink("technology", "ask jack", "Ask Jack blog", "/technology/askjack")
  val internet = SectionLink("technology", "internet", "Internet", "/technology/internet")
  val mobilephones = SectionLink("technology", "mobile phones", "Mobile phones", "/technology/mobilephones")
  val gadgets = SectionLink("technology", "gadgets", "Gadgets", "/technology/gadgets")

  //Business
  val economy = SectionLink("business", "economy", "Economy", "/business")
  val business = economy.copy(title = "business", breadcrumbTitle = "Business")
  val companies = SectionLink("business", "companies", "Companies", "/business/companies")
  val economics = SectionLink("business", "economics", "Economics", "/business/economics")
  val markets = SectionLink("business", "markets", "Markets", "/business/stock-markets")
  val useconomy = SectionLink("business", "US economy", "US economy", "/business/useconomy")
  val recession = SectionLink("business", "recession", "Recession", "/business/recession")
  val investing = SectionLink("business", "investing", "Investing", "/business/investing")
  val banking = SectionLink("business", "banking", "Banking", "/business/banking")
  val marketforceslive = SectionLink("business", "market forces live", "Market Forces live", "/business/marketforceslive")
  val businessblog = SectionLink("business", "business blog", "Business blog", "/business/blog")

  //Money
  val money = SectionLink("money", "money", "Money", "/money")
  val property = SectionLink("money", "property", "Property", "/money/property")
  val houseprices = SectionLink("money", "house prices", "House prices", "/money/houseprices")
  val pensions = SectionLink("money", "pensions", "Pensions", "/money/pensions")
  val savings = SectionLink("money", "savings", "Savings", "/money/savings")
  val borrowing = SectionLink("money", "borrowing", "Borrowing", "/money/debt")
  val insurance = SectionLink("money", "insurance", "Insurance", "/money/insurance")
  val careers = SectionLink("money", "careers", "Careers", "/money/work-and-careers")
  val consumeraffairs = SectionLink("money", "consumer affairs", "Consumer affairs", "/money/consumer-affairs")

  //Life and style
  val lifeandstyle = SectionLink("lifeandstyle", "life", "Life & style", "/lifeandstyle")
  val fashion = SectionLink("lifeandstyle", "fashion", "Fashion", "/fashion")
  val foodanddrink = SectionLink("lifeandstyle", "food", "Food", "/lifeandstyle/food-and-drink")
  val family = SectionLink("lifeandstyle", "family", "family", "/lifeandstyle/family")
  val lostinshowbiz = SectionLink("lifeandstyle", "lost in showbiz", "Lost in showbiz", "/lifeandstyle/lostinshowbiz")
  val women = SectionLink("lifeandstyle", "women", "Women", "/lifeandstyle/women")
  val relationships = SectionLink("lifeandstyle", "relationships", "Relationships", "/lifeandstyle/relationships")
  val healthandwellbeing = SectionLink("lifeandstyle", "health", "Health", "/lifeandstyle/health-and-wellbeing")
  val loveAndSex = SectionLink("lifeandstyle", "love & sex", "Love & sex", "/lifeandstyle/love-and-sex")
  val homeAndGarden = SectionLink("lifeandstyle", "home & garden", "Home & garden", "/lifeandstyle/home-and-garden")

  //Travel
  val travel = SectionLink("travel", "travel", "Travel", "/travel")
  val shortbreaks = SectionLink("travel", "short breaks", "Short breaks", "/travel/short-breaks")
  val uktravel = SectionLink("travel", "UK", "UK", "/travel/uk")
  val europetravel = SectionLink("travel", "europe", "europe", "/travel/europe")
  val usTravel = SectionLink("travel", "US", "US", "/travel/usa")
  val usaTravel = usTravel.copy(title = "USA", breadcrumbTitle = "USA")
  val hotels = SectionLink("travel", "hotels", "Hotels", "/travel/hotels")
  val resturants = SectionLink("travel", "restaurants", "Restaurants", "/travel/restaurants")
  val budget = SectionLink("travel", "budget travel", "Budget travel", "/travel/budget")
  val australasiaTravel = SectionLink("australasia", "australasia", "Australasia", "/travel/australasia")
  val asiaTravel = SectionLink("asia", "asia", "Asia", "/travel/asia")

  //Environment
  val climatechange = SectionLink("environment", "climate change", "Climate change", "/environment/climate-change")
  val wildlife = SectionLink("environment", "wildlife", "Wildlife", "/environment/wildlife")
  val energy = SectionLink("environment", "energy", "Energy", "/environment/energy")
  val conservation = SectionLink("environment", "conservation", "Conservation", "/environment/conservation")
  val food = SectionLink("environment", "food", "Food", "/environment/food")
  val cities = SectionLink("environment", "cities", "Cities", "/cities")
  val globalDevelopment = SectionLink("environment", "development", "Development", "/global-development")

  val footballNav = Seq(
    SectionLink("football", "live scores", "Live scores", "/football/live"),
    SectionLink("football", "tables", "Tables", "/football/tables"),
    SectionLink("football", "competitions", "Competitions", "/football/competitions"),
    SectionLink("football", "results", "Results", "/football/results"),
    SectionLink("football", "fixtures", "Fixtures", "/football/fixtures"),
    SectionLink("football", "clubs", "Clubs", "/football/teams")
  )
}

case class BreadcrumbItem(href: String, title: String)

object Breadcrumbs {
  def items(navigation: Seq[NavItem], page: Content): Seq[BreadcrumbItem] = {
    val primaryKeywod = page.keywordTags.headOption.map(k => BreadcrumbItem(k.url, k.webTitle))
    val firstBreadcrumb = Navigation.topLevelItem(navigation, page).map(n => BreadcrumbItem(n.name.href, n.name.breadcrumbTitle)).orElse(Some(BreadcrumbItem(s"/${page.section}", page.sectionName)))
    val secondBreadcrumb = Navigation.subNav(navigation, page).map(s => BreadcrumbItem(s.href, s.breadcrumbTitle)).orElse(primaryKeywod)
    Seq(firstBreadcrumb, secondBreadcrumb, primaryKeywod).flatten.distinct
  }
}

// helper for the views
object Navigation {

  def topLevelItem(navigation: Seq[NavItem], page: MetaData): Option[NavItem] = page.customSignPosting orElse
    navigation.find(_.exactFor(page)) orElse
    navigation.find(_.currentFor(page)) orElse                /* This searches the top level nav for tags in the page */
    navigation.find(_.currentForIncludingAllTags(page))       /* This searches the whole nav for tags in the page */

  def subNav(navigation: Seq[NavItem], page: MetaData): Option[SectionLink] = topLevelItem(navigation, page).flatMap(_.links.find(_.currentFor(page)))

  def rotatedLocalNav(topSection: NavItem, metaData: MetaData)(implicit request: RequestHeader): Seq[SectionLink] =
    topSection.searchForCurrentSublink(metaData) match {
      case Some(currentSection) =>
        val navSlices = topSection.links.span(_.href != currentSection.href)
        navSlices._2.drop(1) ++ navSlices._1
      case None =>
        topSection.links
    }

  def isEditionFront(topSection: NavItem): Boolean = ("/" :: Edition.editionFronts).contains(topSection.name.href)

  def localLinks(navigation: Seq[NavItem], metaData: MetaData): Seq[SectionLink] = Navigation.topLevelItem(navigation, metaData).map(_.links).getOrElse(List())
}
