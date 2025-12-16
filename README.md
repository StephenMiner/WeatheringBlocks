# Weathering Blocks
With this plugin, you can give any set of blocks transitions just like how copper weathers!  
  
For example, we can have a transition of Stone Bricks turn into either Cracked Stone Bricks, or Mossy stone bricks.    
  
# Config Format
``blocks.yml``  
```yaml
transitions:
  stone_bricks:
    group: stone,1,true,true
    pre-chance: 0.05688889
    chance: 0.75
    states:
    - "cracked_stone_bricks, 0.8, range;4, water;-0.6;true"
    - "mossy_stone_bricks, 0.2, range;4, ratio;true, water;0.6"
```  
The "stone_bricks" key defines what block type this transition should apply to, in this case stone_bricks.  
  
``group: stone,1,true,true``  
This line says that this transition has a group id of "stone", and is transition stage 1. The first 'true' value handles whether 
the probability of block transitions are lower if there are other blocks in the same transition group nearby (default = true).  
The second 'true' refers to whether detecting a block belonging to a later transition stage in the same group
nearby should completely block transitions or not (default = true).  
  
Note that your transition stage value cannot be less than 1. It will cause some problems
with the glue feature if you do this. 1 is considered to be the "most repaired" state.
  
``pre-chance: 0.05688889``  
This line defines what the probability is that given that a block that we have
a transition for gets randomly ticked, that we will proceed to attempt the transition process. 
Basically it is a roll we make to decide whether we will try to roll for a transition or not. 
This is the default value for copper.  
  
``chance: 0.75``  
This line defines the base chance for the transition to occur once we pass the
"pre-chance" check. This chance may be modified by if the options to lower the chance
based on nearby blocks in the same group of later stages, or set to 0 if the option for 
blocking transitions if a block of a later transition is present nearby.  
  
```yaml
states:
   - "cracked_stone_bricks, 0.8, range;4, water;-0.6;true"
   - "mossy_stone_bricks, 0.2, range;4, water;0.6;true"
```  
Each entry defines a potential state we may transition to. 
The first item 'cracked_stone_bricks' refers to the material to transition to. 
The second defines this material's weight for being selected.  
  
``range;4``  
refers to the default distance that the plugin will check around the block
for any distance related calculations such as if you have the option for 
delaying block transitions based on nearby blocks on or not.  
  
``water;-0.6;true``  
This is an example of a ProbabilityFlag.  
  
The first item "water" is the material that you want to scan for.  
  
The second item "-0.6" represents how you want detecting this block to modify this 
potential state's weight for being selected.  
  
The third item "true" defines whether you want this modifier to be applied in full when 1 block
of water or whatever material set is detected, or if you want it to be multiplied by the ratio of water blocks vs
non water blocks in the range of our scan.  
  
You can have as many probability flags as you'd like.  
  
You may also have as many potential states as you want too. 
  
# Glue
The glue is pretty simple, you modify the parameters pre-defined in the ``settings.yml``
file.  
The idea behind glue is that you can use it to restore blocks to their former states.  
There are two primary options to worry about:  
``full-repair: true``  
  
If this option is true, then when you try to fix a block, it will look for the transition in the 
same group as the block to fix with transition state 1, and set this block's material to that one.  
If This is false, then when you try to fix a block, it restored to a transition whose transition stage that is one less 
than the current one. If multiple transitions satisfy this, then one is selected at random unless 
  
``effect-area: 4``  
  
This option defines the radius around the clicked block that the glue will affect.










# The Technical Side of things
How it works is nearly identical to copper weathering. If an appropriate block is "ticked" by minecraft's
random tick, we then perform the following:  
1) We roll a number and check if it is less than this block's pre-chance for a transition.
In other words, the probability that we will consider advancing this block to its next state.  
2) Next, we roll another number. If you left all defaults enabled for your 
transition, the following will happen.
    - If grouping-delay is true, then we scan the nearby area for blocks of either this block's current "weathering-stage"
   or above or belay it. We then multiply the square of the ratio of the amount of blocks whose 
   weathering stage is greater than the block that we are looking at to all of blocks that fall under this block's weathering group 
   with our BlockTransition's transition chance.
    - Note for the above that if lowerTransitionBlocking is true, then the presence of a block
   at a lower weathering stage in the same group as the current block, will force the state transition to fail.
    - If grouping-delay is false, then we simply use our base chance value.
3) If our rolled number was less than or equal to our chance from (2), then
we move on to selecting what material to change the block to. This is done through weighted selection. All weights are summed, and then 
we roll a number. We then progressively sum our weights from smallest to largest until the roll is less than the sum.  
4) Finally the block transitions to the selected type
