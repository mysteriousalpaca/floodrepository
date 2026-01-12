<hx>**Project Overview**</hx>


CREATOR:
Dylan, a beginner coder taking a computer science course in high school. 

Flood Defense Simulation is a strategy-based disaster management game inspired by city-builder and simulation games. The player is tasked with protecting critical infrastructure on a 20×20 grid-based map from multiple types of flooding, including water, toxic floods, acidic rain, and a final extreme hazard: **fluoroantimonic acid**.

The game is intended to be focused around strategic planning under material/currency constraints. Players must manage a limited budget, analyze terrain elevation, choose appropriate defensive materials, and prepare before the flood simulation begins. Once the preparation phase ends, the flood propagates across the map using an advanced flood-fill algorithm that accounts for elevation, wall resistance, and multiple flood sources.


This project is intended to:
- Include a more advanced form of a flood-fill algorithm, which waits or delays spread based on terrain elevation and ascending and descending
- Have flood collision for two or more water sources colliding with each other
- Have appealing 2.5d visuals that extend beyond the typical flat grid style top-down game
- Have a gui and currency system that has unique elements and tradeoffs for each material
- Support multiple levels with unique objectives and increasing difficulty

3 PHASES:
Planning Phase:
- Review objectives
- Place walls and upgrades
- Manage budget
Simulation Phase:
- Flood begins from one or more directions
- Flood spreads dynamically across terrain
Evaluation Phase:
- Objectives are checked depending on importance
- Score is calculated
- Player advances or redoes level 
