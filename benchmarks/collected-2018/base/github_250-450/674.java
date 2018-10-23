// https://searchcode.com/api/result/93557280/

package com.johngalt.gulch.tileentities.common;

import com.johngalt.gulch.items.GaltItems;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;

/**
 * Created on 6/14/2014.
 */
public class GaltTileEntityContainer extends TileEntity implements IInventory
{
    private ItemStack[] inventory;
    private int INVENTORY_SIZE = 7;
    private boolean isSmashing = false;
    private Block OUTPUT_ITEM = Blocks.dirt;
    private int smashDuration = 200;
    public int smashTime = smashDuration; // The total smash time
    public int smashTimeRemaining; // Value to store the remaining smash time

    public GaltTileEntityContainer()
    {
        inventory = new ItemStack[INVENTORY_SIZE];
    }

    @Override
    public int getSizeInventory()
    {
        return inventory.length;
    }

    @Override
    public ItemStack getStackInSlot(int slotIndex)
    {
        return inventory[slotIndex];
    }

    /*
    Decreases the stacksize.
    If the stack is not null, and the amount to decrease is more than the slot has, set to null
    else if the stack is not null, split the stack. If that makes the inv 0, make it null.
     */
    @Override
    public ItemStack decrStackSize(int slotIndex, int decrementAmount)
    {
        ItemStack itemStack = getStackInSlot(slotIndex);
        if (itemStack != null)
        {
            if (itemStack.stackSize <= decrementAmount)
            {
                setInventorySlotContents(slotIndex, null);
            }
            else
            {
                itemStack = itemStack.splitStack(decrementAmount);
                if (itemStack.stackSize == 0)
                {
                    setInventorySlotContents(slotIndex, null);
                }
            }
        }
        return itemStack;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int slotIndex)
    {
        ItemStack itemStack = getStackInSlot(slotIndex);
        if (itemStack != null)
        {
            setInventorySlotContents(slotIndex, null);
        }
        return itemStack;
    }

    @Override
    public void setInventorySlotContents(int slotIndex, ItemStack itemStack)
    {
        inventory[slotIndex] = itemStack;
        if (itemStack != null && itemStack.stackSize > getInventoryStackLimit())
        {
            itemStack.stackSize = getInventoryStackLimit();
        }
    }

    @Override
    public String getInventoryName()
    {
        return "notused";
    }

    @Override
    public boolean hasCustomInventoryName()
    {
        return true;
    }

    @Override
    public int getInventoryStackLimit()
    {
        return 64;
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer var1)
    {
        return true;
    }

    @Override
    public void openInventory()
    {

    }

    @Override
    public void closeInventory()
    {

    }

    @Override
    public boolean isItemValidForSlot(int var1, ItemStack var2)
    {
        return true;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbtTagCompound)
    {
        super.readFromNBT(nbtTagCompound);
        NBTTagList nbttaglist = nbtTagCompound.getTagList("Items", 10); // 10 = NBTTagCompound
        this.inventory = new ItemStack[this.getSizeInventory()];

        for (int i = 0; i < nbttaglist.tagCount(); ++i)
        {
            NBTTagCompound nbttagcompound = nbttaglist.getCompoundTagAt(i);
            int j = nbttagcompound.getByte("Slot");

            if (j >= 0 && j < this.inventory.length)
            {
                this.inventory[j] = ItemStack.loadItemStackFromNBT(nbttagcompound);
            }
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound nbtTagCompound)
    {
        super.writeToNBT(nbtTagCompound);
        NBTTagList nbttaglist = new NBTTagList();

        for (int i = 0; i < this.inventory.length; ++i)
        {
            if (this.inventory[i] != null)
            {
                NBTTagCompound nbttagcompound = new NBTTagCompound();
                nbttagcompound.setByte("Slot", (byte) i);
                this.inventory[i].writeToNBT(nbttagcompound);
                nbttaglist.appendTag(nbttagcompound);
            }
        }
        nbtTagCompound.setTag("Items", nbttaglist);
    }

    @Override
    public void updateEntity()
    {
        //If on server side
        if (!worldObj.isRemote)
        {
            //If the machine is already smashing
            if (isSmashing)
            {
                //And the te is done smashing
                if (smashTimeRemaining == 0)
                {
                    //Null pointer blocker
                    if (inventory[0] != null)
                    {
                        //null pointer blocker
                        if (inventory[6] != null)
                        {
                            //If the output slot has a dirt already, add one
                            if (Block.getBlockFromItem(inventory[6].getItem()) == OUTPUT_ITEM)
                            {
                                inventory[6].stackSize++;

                                //if input slot is not empty, decrease
                                if (inventory[0] != null) this.decrStackSize(0, 1);
                            }
                        }
                        // else put one item in
                        else
                        {
                            inventory[6] = new ItemStack(OUTPUT_ITEM, 1);

                            //if input slot is not empty, decrease
                            if (inventory[0] != null) this.decrStackSize(0, 1);
                        }
                    }
                    //done smashing, so set isSmashing to false
                    isSmashing = false;
                    smashTime = smashDuration;
                }
                //reduce smash time
                smashTimeRemaining--;
            }

            //If not smashing, check if can smash
            else if (canSmash())
            {
                //If te can smash, and output slot is not full
                if (inventory[6] == null || inventory[6].stackSize != 64)
                {
                    //turn smashing on
                    isSmashing = true;
                    smashTimeRemaining = smashDuration;
                    smashTime = smashDuration;
                }
            }
        }
    }

    /**
     * Checks all 6 slots for a valid input for the te
     *
     * @return canSmash
     */
    public boolean canSmash()
    {
        //For all 6 slots
        for (int i = 0; i < 6; i++)
        {
            //Null pointer blocker
            if (inventory[i] != null)
            {
                //If the item isn't null, and the item is my testItem (item that can be smashed) and the stack size > 0
                if (inventory[i].getItem() != null && inventory[i].getItem() == GaltItems.simpleItem && inventory[i].stackSize != 0)
                {
                    //copy the found slot
                    ItemStack replacer = inventory[i].copy();
                    //set the slot found to null
                    inventory[i] = null;
                    //replace the first slot with the found slot
                    inventory[0] = replacer;
                    //return true
                    return true;
                }
            }
        }
        return false;
    }

    public int getSmashTimeRemaining()
    {
        return smashTimeRemaining;
    }

    public int getSmashTime()
    {
        return smashTime;
    }
}

