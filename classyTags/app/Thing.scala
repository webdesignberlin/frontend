package tags

sealed trait Thing {
  def name: String
}

case class Place(name: String) extends Thing
case class Event(name: String) extends Thing
case class Person(name: String) extends Thing