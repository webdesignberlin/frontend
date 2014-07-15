package tags

trait Thing {
  def thingType: String
  def name: String
}

class Place(val name: String) extends Thing {
  override lazy val thingType = "Place"
}

class Event(val name: String) extends Thing {
  override lazy val thingType = "Event"
}

class Person(val name: String) extends Thing {
  override lazy val thingType = "Person"
}