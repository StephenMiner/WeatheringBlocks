# Weathering Blocks
With this plugin, you can give any set of blocks transitions just like how copper weathers!  
  
For example, we can have a transition of Stone Bricks turn into either Cracked Stone Bricks, or Mossy stone bricks.    
  
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
we move on to selecting what material to change the block to. 