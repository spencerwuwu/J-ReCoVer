// https://searchcode.com/api/result/70981182/

package com.fpbm.fee.model.original;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

import org.hi.framework.model.BaseObject;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import com.fpbm.common.util.DateUtil;
import com.fpbm.fee.model.FeeSettle;
import com.fpbm.reserve.model.ResBusiness;
import org.hi.base.organization.model.HiUser;

public abstract class FeeSettleAbstract extends BaseObject implements Serializable{

 	
 	/**
	 * id
	 */	
	protected  Integer id;

	/**
	 * version
	 */	
 	protected  Integer version;

 	 /**
	 * 
	 */	
 	protected  ResBusiness businessId;

 	 /**
	 * 
	 */	
 	protected  Double sum;

 	 /**
	 * 
	 */	
 	protected  Double reduce;

 	 /**
	 * 
	 */	
 	protected  Double realSum;

 	 /**
	 * 
	 */	
 	protected  Integer passFlag;

 	 /**
	 * 
	 */	
 	protected  Integer settleFlag = 400301;//

 	 /**
	 * 
	 */	
 	protected  Double backFee;

 	 /**
	 * 
	 */	
 	protected  Integer backPassFlag;

 	 /**
	 * 
	 */	
 	protected  Integer invalidFlag;

 	 /**
	 * 
	 */	
 	protected  String remark;

 	 /**
	 * 
	 */	
 	protected  Date createTime = DateUtil.getCurDate();

 	 /**
	 * ID
	 */	
 	protected  HiUser lastUpdateUserId;

 	 /**
	 * 
	 */	
 	protected  Date lastUpdateTime;

 	 /**
	 * 
	 */	
 	protected  HiUser creator = org.hi.framework.security.context.UserContextHelper.getUser();

 	 /**
	 * 
	 */	
 	protected  Integer deleted = 0;


    public Integer getId() {
        return this.id;
    }
    
    public void setId(Integer id) {
    		if((id != null && this.id == null) || 
				this.id != null && (!this.id.equals(id) || id == null)){
        		this.setDirty(true);
        		this.oldValues.put("id", this.id);
        	}
        this.id = id;
    }
    
     public Integer getVersion() {
        return this.version;
    }
    
    public void setVersion(Integer version) {
    		if((version != null && this.version == null) || 
				this.version != null && (!this.version.equals(version) || version == null)){
        		this.setDirty(true);
        		this.oldValues.put("version", this.version);
        	}
        this.version = version;
    }
    
    public ResBusiness getBusinessId() {
        return this.businessId;
    }
    
    public void setBusinessId(ResBusiness businessId) {
    		if((businessId != null && this.businessId == null) || 
				this.businessId != null && (!this.businessId.equals(businessId) || businessId == null)){
        		this.setDirty(true);
        		this.oldValues.put("businessId", this.businessId);
        	}
        this.businessId = businessId;
    }
    
    public Double getSum() {
        return this.sum;
    }
    
    public void setSum(Double sum) {
    		if((sum != null && this.sum == null) || 
				this.sum != null && (!this.sum.equals(sum) || sum == null)){
        		this.setDirty(true);
        		this.oldValues.put("sum", this.sum);
        	}
        this.sum = sum;
    }
    
    public Double getReduce() {
        return this.reduce;
    }
    
    public void setReduce(Double reduce) {
    		if((reduce != null && this.reduce == null) || 
				this.reduce != null && (!this.reduce.equals(reduce) || reduce == null)){
        		this.setDirty(true);
        		this.oldValues.put("reduce", this.reduce);
        	}
        this.reduce = reduce;
    }
    
    public Double getRealSum() {
        return this.realSum;
    }
    
    public void setRealSum(Double realSum) {
    		if((realSum != null && this.realSum == null) || 
				this.realSum != null && (!this.realSum.equals(realSum) || realSum == null)){
        		this.setDirty(true);
        		this.oldValues.put("realSum", this.realSum);
        	}
        this.realSum = realSum;
    }
    
    public Integer getPassFlag() {
        return this.passFlag;
    }
    
    public void setPassFlag(Integer passFlag) {
    		if((passFlag != null && this.passFlag == null) || 
				this.passFlag != null && (!this.passFlag.equals(passFlag) || passFlag == null)){
        		this.setDirty(true);
        		this.oldValues.put("passFlag", this.passFlag);
        	}
        this.passFlag = passFlag;
    }
    
    public Integer getSettleFlag() {
        return this.settleFlag;
    }
    
    public void setSettleFlag(Integer settleFlag) {
    		if((settleFlag != null && this.settleFlag == null) || 
				this.settleFlag != null && (!this.settleFlag.equals(settleFlag) || settleFlag == null)){
        		this.setDirty(true);
        		this.oldValues.put("settleFlag", this.settleFlag);
        	}
        this.settleFlag = settleFlag;
    }
    
    public Double getBackFee() {
        return this.backFee;
    }
    
    public void setBackFee(Double backFee) {
    		if((backFee != null && this.backFee == null) || 
				this.backFee != null && (!this.backFee.equals(backFee) || backFee == null)){
        		this.setDirty(true);
        		this.oldValues.put("backFee", this.backFee);
        	}
        this.backFee = backFee;
    }
    
    public Integer getBackPassFlag() {
        return this.backPassFlag;
    }
    
    public void setBackPassFlag(Integer backPassFlag) {
    		if((backPassFlag != null && this.backPassFlag == null) || 
				this.backPassFlag != null && (!this.backPassFlag.equals(backPassFlag) || backPassFlag == null)){
        		this.setDirty(true);
        		this.oldValues.put("backPassFlag", this.backPassFlag);
        	}
        this.backPassFlag = backPassFlag;
    }
    
    public Integer getInvalidFlag() {
        return this.invalidFlag;
    }
    
    public void setInvalidFlag(Integer invalidFlag) {
    		if((invalidFlag != null && this.invalidFlag == null) || 
				this.invalidFlag != null && (!this.invalidFlag.equals(invalidFlag) || invalidFlag == null)){
        		this.setDirty(true);
        		this.oldValues.put("invalidFlag", this.invalidFlag);
        	}
        this.invalidFlag = invalidFlag;
    }
    
    public String getRemark() {
        return this.remark;
    }
    
    public void setRemark(String remark) {
    		if((remark != null && this.remark == null) || 
				this.remark != null && (!this.remark.equals(remark) || remark == null)){
        		this.setDirty(true);
        		this.oldValues.put("remark", this.remark);
        	}
        this.remark = remark;
    }
    
    public Date getCreateTime() {
        return this.createTime;
    }
    
    public void setCreateTime(Date createTime) {
    		if((createTime != null && this.createTime == null) || 
				this.createTime != null && (!this.createTime.equals(createTime) || createTime == null)){
        		this.setDirty(true);
        		this.oldValues.put("createTime", this.createTime);
        	}
        this.createTime = createTime;
    }
    
    public HiUser getLastUpdateUserId() {
        return this.lastUpdateUserId;
    }
    
    public void setLastUpdateUserId(HiUser lastUpdateUserId) {
    		if((lastUpdateUserId != null && this.lastUpdateUserId == null) || 
				this.lastUpdateUserId != null && (!this.lastUpdateUserId.equals(lastUpdateUserId) || lastUpdateUserId == null)){
        		this.setDirty(true);
        		this.oldValues.put("lastUpdateUserId", this.lastUpdateUserId);
        	}
        this.lastUpdateUserId = lastUpdateUserId;
    }
    
    public Date getLastUpdateTime() {
        return this.lastUpdateTime;
    }
    
    public void setLastUpdateTime(Date lastUpdateTime) {
    		if((lastUpdateTime != null && this.lastUpdateTime == null) || 
				this.lastUpdateTime != null && (!this.lastUpdateTime.equals(lastUpdateTime) || lastUpdateTime == null)){
        		this.setDirty(true);
        		this.oldValues.put("lastUpdateTime", this.lastUpdateTime);
        	}
        this.lastUpdateTime = lastUpdateTime;
    }
    
    public HiUser getCreator() {
        return this.creator;
    }
    
    public void setCreator(HiUser creator) {
    		if((creator != null && this.creator == null) || 
				this.creator != null && (!this.creator.equals(creator) || creator == null)){
        		this.setDirty(true);
        		this.oldValues.put("creator", this.creator);
        	}
        this.creator = creator;
    }
    
    public Integer getDeleted() {
        return this.deleted;
    }
    
    public void setDeleted(Integer deleted) {
    		if((deleted != null && this.deleted == null) || 
				this.deleted != null && (!this.deleted.equals(deleted) || deleted == null)){
        		this.setDirty(true);
        		this.oldValues.put("deleted", this.deleted);
        	}
        this.deleted = deleted;
    }
    


   public boolean equals(Object other) {
         if ( (this == other ) ) return true;
		 if ( (other == null ) ) return false;
		 if ( !(other instanceof FeeSettle) ) return false;
		 FeeSettle castOther = ( FeeSettle ) other; 
		 
		 return  ( (this.getId()==castOther.getId()) || ( this.getId()!=null && castOther.getId()!=null && this.getId().equals(castOther.getId()) ) );
   }
   
   public int hashCode() {
        HashCodeBuilder hcb = new HashCodeBuilder(17, 37);
        hcb.append(getId());
		hcb.append("FeeSettle".hashCode());
        return hcb.toHashCode();
    }

   public String toString() {
       ToStringBuilder sb = new ToStringBuilder(this, ToStringStyle.DEFAULT_STYLE);
       sb.append("id", this.id)
		.append("sum", this.sum)
		.append("reduce", this.reduce)
		.append("realSum", this.realSum)
		.append("backFee", this.backFee)
		.append("remark", this.remark)
		.append("deleted", this.deleted);
      
        return sb.toString();
   }

   public Serializable getPrimarykey(){
   		return id;
   }



}
