package tags

sealed trait Thing {
  def name: String
  def category: String
}

case class Place(name: String, category: String) extends Thing
case class Event(name: String, category: String) extends Thing
case class Person(name: String, category: String) extends Thing
case class Unknown(name: String, category: String) extends Thing