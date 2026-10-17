(define (domain sokoban)

  (:requirements :strips :typing)

  (:types location)

  (:predicates
    (clear ?l - location)
    (at-robot ?l - location)
    (at-box ?l - location)
    (adj-u ?from - location ?to - location)
    (adj-d ?from - location ?to - location)
    (adj-l ?from - location ?to - location)
    (adj-r ?from - location ?to - location)
    (safe-for-box ?l - location)
  )

  (:action move-u
    :parameters (?from - location ?to - location)
    :precondition (and (at-robot ?from) (adj-u ?from ?to) (clear ?to))
    :effect (and (not (at-robot ?from)) (at-robot ?to) (clear ?from) (not (clear ?to)))
  )
  (:action move-d
    :parameters (?from - location ?to - location)
    :precondition (and (at-robot ?from) (adj-d ?from ?to) (clear ?to))
    :effect (and (not (at-robot ?from)) (at-robot ?to) (clear ?from) (not (clear ?to)))
  )
  (:action move-l
    :parameters (?from - location ?to - location)
    :precondition (and (at-robot ?from) (adj-l ?from ?to) (clear ?to))
    :effect (and (not (at-robot ?from)) (at-robot ?to) (clear ?from) (not (clear ?to)))
  )
  (:action move-r
    :parameters (?from - location ?to - location)
    :precondition (and (at-robot ?from) (adj-r ?from ?to) (clear ?to))
    :effect (and (not (at-robot ?from)) (at-robot ?to) (clear ?from) (not (clear ?to)))
  )

  (:action push-u
    :parameters (?robot - location ?box - location ?dest - location)
    :precondition (and (at-robot ?robot) (at-box ?box) (adj-u ?robot ?box) (adj-u ?box ?dest) (clear ?dest) (safe-for-box ?dest))
    :effect (and (not (at-robot ?robot)) (at-robot ?box) (not (at-box ?box)) (at-box ?dest) (clear ?robot) (not (clear ?dest)))
  )
  (:action push-d
    :parameters (?robot - location ?box - location ?dest - location)
    :precondition (and (at-robot ?robot) (at-box ?box) (adj-d ?robot ?box) (adj-d ?box ?dest) (clear ?dest) (safe-for-box ?dest))
    :effect (and (not (at-robot ?robot)) (at-robot ?box) (not (at-box ?box)) (at-box ?dest) (clear ?robot) (not (clear ?dest)))
  )
  (:action push-l
    :parameters (?robot - location ?box - location ?dest - location)
    :precondition (and (at-robot ?robot) (at-box ?box) (adj-l ?robot ?box) (adj-l ?box ?dest) (clear ?dest) (safe-for-box ?dest))
    :effect (and (not (at-robot ?robot)) (at-robot ?box) (not (at-box ?box)) (at-box ?dest) (clear ?robot) (not (clear ?dest)))
  )
  (:action push-r
    :parameters (?robot - location ?box - location ?dest - location)
    :precondition (and (at-robot ?robot) (at-box ?box) (adj-r ?robot ?box) (adj-r ?box ?dest) (clear ?dest) (safe-for-box ?dest))
    :effect (and (not (at-robot ?robot)) (at-robot ?box) (not (at-box ?box)) (at-box ?dest) (clear ?robot) (not (clear ?dest)))
  )
)
