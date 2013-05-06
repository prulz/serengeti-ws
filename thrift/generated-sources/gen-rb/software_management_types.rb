#
# Autogenerated by Thrift Compiler (0.9.0)
#
# DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
#

require 'thrift'

module Software
  module Mgmt
    module Thrift
      module ClusterAction
        QUERY = 1
        CREATE = 2
        UPDATE = 3
        START = 4
        STOP = 5
        DESTROY = 6
        CONFIGURE = 7
        CONFIGURE_HARDWARE = 8
        ENABLE_CHEF_CLIENT = 9
        DISABLE_CHEF_CLIENT = 10
        VALUE_MAP = {1 => "QUERY", 2 => "CREATE", 3 => "UPDATE", 4 => "START", 5 => "STOP", 6 => "DESTROY", 7 => "CONFIGURE", 8 => "CONFIGURE_HARDWARE", 9 => "ENABLE_CHEF_CLIENT", 10 => "DISABLE_CHEF_CLIENT"}
        VALID_VALUES = Set.new([QUERY, CREATE, UPDATE, START, STOP, DESTROY, CONFIGURE, CONFIGURE_HARDWARE, ENABLE_CHEF_CLIENT, DISABLE_CHEF_CLIENT]).freeze
      end

      module ServerStatus
        VM_READY = 1
        SERVICE_READY = 2
        BOOTSTRAP_FAILED = 3
        VALUE_MAP = {1 => "VM_READY", 2 => "SERVICE_READY", 3 => "BOOTSTRAP_FAILED"}
        VALID_VALUES = Set.new([VM_READY, SERVICE_READY, BOOTSTRAP_FAILED]).freeze
      end

      class ServerData
        include ::Thrift::Struct, ::Thrift::Struct_Union
        ACTION = 1
        BOOTSTRAPPED = 2
        CREATED = 3
        DELETED = 4
        ERROR_CODE = 5
        ERROR_MSG = 6
        FINISHED = 7
        HA = 8
        HOSTNAME = 9
        IPADDRESS = 10
        NAME = 11
        PHYSICALHOST = 12
        PROGRESS = 13
        RACK = 14
        STATUS = 15
        SUCCEED = 16

        FIELDS = {
          ACTION => {:type => ::Thrift::Types::STRING, :name => 'action', :optional => true},
          BOOTSTRAPPED => {:type => ::Thrift::Types::BOOL, :name => 'bootstrapped', :optional => true},
          CREATED => {:type => ::Thrift::Types::BOOL, :name => 'created', :optional => true},
          DELETED => {:type => ::Thrift::Types::BOOL, :name => 'deleted', :optional => true},
          ERROR_CODE => {:type => ::Thrift::Types::I32, :name => 'error_code', :optional => true},
          ERROR_MSG => {:type => ::Thrift::Types::STRING, :name => 'error_msg', :optional => true},
          FINISHED => {:type => ::Thrift::Types::BOOL, :name => 'finished', :optional => true},
          HA => {:type => ::Thrift::Types::BOOL, :name => 'ha', :optional => true},
          HOSTNAME => {:type => ::Thrift::Types::STRING, :name => 'hostName', :optional => true},
          IPADDRESS => {:type => ::Thrift::Types::STRING, :name => 'ipAddress', :optional => true},
          NAME => {:type => ::Thrift::Types::STRING, :name => 'name', :optional => true},
          PHYSICALHOST => {:type => ::Thrift::Types::STRING, :name => 'physicalHost', :optional => true},
          PROGRESS => {:type => ::Thrift::Types::I32, :name => 'progress', :optional => true},
          RACK => {:type => ::Thrift::Types::STRING, :name => 'rack', :optional => true},
          STATUS => {:type => ::Thrift::Types::STRING, :name => 'status', :optional => true},
          SUCCEED => {:type => ::Thrift::Types::BOOL, :name => 'succeed', :optional => true}
        }

        def struct_fields; FIELDS; end

        def validate
        end

        ::Thrift::Struct.generate_accessors self
      end

      class GroupData
        include ::Thrift::Struct, ::Thrift::Struct_Union
        GROUPNAME = 1
        INSTANCES = 2

        FIELDS = {
          GROUPNAME => {:type => ::Thrift::Types::STRING, :name => 'groupName', :optional => true},
          INSTANCES => {:type => ::Thrift::Types::LIST, :name => 'instances', :element => {:type => ::Thrift::Types::STRUCT, :class => ::Software::Mgmt::Thrift::ServerData}, :optional => true}
        }

        def struct_fields; FIELDS; end

        def validate
        end

        ::Thrift::Struct.generate_accessors self
      end

      class ClusterData
        include ::Thrift::Struct, ::Thrift::Struct_Union
        CLUSTERNAME = 1
        GROUPS = 2

        FIELDS = {
          CLUSTERNAME => {:type => ::Thrift::Types::STRING, :name => 'clusterName', :optional => true},
          GROUPS => {:type => ::Thrift::Types::MAP, :name => 'groups', :key => {:type => ::Thrift::Types::STRING}, :value => {:type => ::Thrift::Types::STRUCT, :class => ::Software::Mgmt::Thrift::GroupData}, :optional => true}
        }

        def struct_fields; FIELDS; end

        def validate
        end

        ::Thrift::Struct.generate_accessors self
      end

      # Operation Status data structure
      class OperationStatus
        include ::Thrift::Struct, ::Thrift::Struct_Union
        FINISHED = 1
        SUCCEED = 2
        PROGRESS = 3
        ERROR_MSG = 4
        TOTAL = 5
        SUCCESS = 6
        FAILURE = 7
        RUNNING = 8

        FIELDS = {
          FINISHED => {:type => ::Thrift::Types::BOOL, :name => 'finished', :optional => true},
          SUCCEED => {:type => ::Thrift::Types::BOOL, :name => 'succeed', :optional => true},
          PROGRESS => {:type => ::Thrift::Types::I32, :name => 'progress', :optional => true},
          ERROR_MSG => {:type => ::Thrift::Types::STRING, :name => 'error_msg', :optional => true},
          TOTAL => {:type => ::Thrift::Types::I32, :name => 'total', :optional => true},
          SUCCESS => {:type => ::Thrift::Types::I32, :name => 'success', :optional => true},
          FAILURE => {:type => ::Thrift::Types::I32, :name => 'failure', :optional => true},
          RUNNING => {:type => ::Thrift::Types::I32, :name => 'running', :optional => true}
        }

        def struct_fields; FIELDS; end

        def validate
        end

        ::Thrift::Struct.generate_accessors self
      end

      class OperationStatusWithDetail
        include ::Thrift::Struct, ::Thrift::Struct_Union
        OPERATIONSTATUS = 1
        CLUSTERDATA = 2

        FIELDS = {
          OPERATIONSTATUS => {:type => ::Thrift::Types::STRUCT, :name => 'operationStatus', :class => ::Software::Mgmt::Thrift::OperationStatus, :optional => true},
          CLUSTERDATA => {:type => ::Thrift::Types::STRUCT, :name => 'clusterData', :class => ::Software::Mgmt::Thrift::ClusterData, :optional => true}
        }

        def struct_fields; FIELDS; end

        def validate
        end

        ::Thrift::Struct.generate_accessors self
      end

      # Cluster operation data structure
      class ClusterOperation
        include ::Thrift::Struct, ::Thrift::Struct_Union
        ACTION = 1
        TARGETNAME = 2
        SPECFILENAME = 3
        LOGLEVEL = 4

        FIELDS = {
          ACTION => {:type => ::Thrift::Types::I32, :name => 'action', :enum_class => ::Software::Mgmt::Thrift::ClusterAction},
          TARGETNAME => {:type => ::Thrift::Types::STRING, :name => 'targetName'},
          SPECFILENAME => {:type => ::Thrift::Types::STRING, :name => 'specFileName'},
          LOGLEVEL => {:type => ::Thrift::Types::STRING, :name => 'logLevel', :optional => true}
        }

        def struct_fields; FIELDS; end

        def validate
          raise ::Thrift::ProtocolException.new(::Thrift::ProtocolException::UNKNOWN, 'Required field action is unset!') unless @action
          raise ::Thrift::ProtocolException.new(::Thrift::ProtocolException::UNKNOWN, 'Required field targetName is unset!') unless @targetName
          raise ::Thrift::ProtocolException.new(::Thrift::ProtocolException::UNKNOWN, 'Required field specFileName is unset!') unless @specFileName
          unless @action.nil? || ::Software::Mgmt::Thrift::ClusterAction::VALID_VALUES.include?(@action)
            raise ::Thrift::ProtocolException.new(::Thrift::ProtocolException::UNKNOWN, 'Invalid value of field action!')
          end
        end

        ::Thrift::Struct.generate_accessors self
      end

      class ClusterOperationException < ::Thrift::Exception
        include ::Thrift::Struct, ::Thrift::Struct_Union
        def initialize(message=nil)
          super()
          self.message = message
        end

        MESSAGE = 1

        FIELDS = {
          MESSAGE => {:type => ::Thrift::Types::STRING, :name => 'message'}
        }

        def struct_fields; FIELDS; end

        def validate
        end

        ::Thrift::Struct.generate_accessors self
      end

    end
  end
end
